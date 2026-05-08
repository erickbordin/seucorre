package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.usuario.domain.Usuario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Service
public class GeradorPlanoIA {

    private static final BigDecimal LIMITE_REGRA_DEZ_POR_CENTO = BigDecimal.valueOf(1.10d);
    private static final int SCALE_DISTANCIA = 2;

    private final IAClient iaClient;
    private final PromptBuilder promptBuilder;

    public GeradorPlanoIA(IAClient iaClient, PromptBuilder promptBuilder) {
        if (iaClient == null) {
            throw new IllegalArgumentException("IAClient é obrigatório.");
        }
        if (promptBuilder == null) {
            throw new IllegalArgumentException("PromptBuilder é obrigatório.");
        }
        this.iaClient = iaClient;
        this.promptBuilder = promptBuilder;
    }

    public PlanoTreino gerarPlano(Usuario usuario, Objetivo objetivo) {
        return gerarPlano(usuario, objetivo, List.of());
    }

    public PlanoTreino gerarPlano(Usuario usuario, Objetivo objetivo, List<PlanoTreino> historicoPlanos) {
        validarUsuario(usuario);

        Objetivo objetivoEfetivo = objetivo != null ? objetivo : usuario.getObjetivo();
        String prompt = construirContexto(usuario, objetivoEfetivo, historicoPlanos);
        String resposta = iaClient.gerarResposta(prompt);

        PlanoTreino planoTreino = parsearResposta(resposta, usuario);
        aplicarRegraDosDezPorCento(planoTreino);
        return planoTreino;
    }

    public PlanoTreino reescreverPlano(PlanoTreino plano, CheckinSemanal checkin) {
        return reescreverPlano(plano, checkin, List.of());
    }

    public PlanoTreino reescreverPlano(PlanoTreino plano, CheckinSemanal checkin, List<ProgressoSemanal> progressos) {
        if (plano == null) {
            throw new IllegalArgumentException("Plano é obrigatório.");
        }
        if (checkin == null) {
            throw new IllegalArgumentException("Check-in é obrigatório.");
        }
        if (!checkin.precisaReescreverPlano()) {
            if (checkin.getPlanoReescrito() == null || checkin.getPlanoReescrito()) {
                checkin.setPlanoReescrito(false);
            }
            return plano;
        }

        String prompt = promptBuilder.construirPromptReescrita(plano, checkin, progressos);
        String resposta = iaClient.gerarResposta(prompt);

        PlanoTreino planoReescrito = parsearResposta(resposta, plano.getUsuario());
        herdarMetadadosBasicos(plano, planoReescrito);
        aplicarRegraDosDezPorCento(planoReescrito);
        checkin.setPlanoReescrito(true);
        return planoReescrito;
    }

    public String gerarAnaliseCheckin(CheckinSemanal checkin) {
        if (checkin == null) {
            throw new IllegalArgumentException("Check-in é obrigatório.");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce analisa check-ins semanais do SeuCorre.\n");
        prompt.append("Contexto do check-in:\n");
        prompt.append("- ").append(checkin.gerarContextoParaIA()).append('\n');
        prompt.append("Tarefa:\n");
        prompt.append("- classifique o risco atual do corredor\n");
        prompt.append("- identifique sobrecarga, dor e recuperacao insuficiente\n");
        prompt.append("- explique se o plano precisa ser reescrito\n");
        prompt.append("- responda em texto curto e objetivo\n");
        return iaClient.gerarResposta(prompt.toString());
    }

    public String sugerirAjuste(ProgressoSemanal progresso) {
        if (progresso == null) {
            throw new IllegalArgumentException("Progresso semanal é obrigatório.");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce sugere ajustes de treino no SeuCorre.\n");
        prompt.append("Resumo do progresso semanal:\n");
        prompt.append("- ").append(progresso.gerarResumo()).append('\n');
        prompt.append("Tarefa:\n");
        prompt.append("- sugerir ajuste pratico de volume, intensidade e distribuicao semanal\n");
        prompt.append("- respeitar a regra dos 10 por cento\n");
        prompt.append("- responder em texto curto e objetivo\n");
        return iaClient.gerarResposta(prompt.toString());
    }

    protected String construirContexto(Usuario usuario) {
        return construirContexto(usuario, usuario == null ? null : usuario.getObjetivo());
    }

    protected PlanoTreino parsearResposta(String json) {
        return parsearResposta(json, null);
    }

    private String construirContexto(Usuario usuario, Objetivo objetivo) {
        return construirContexto(usuario, objetivo, List.of());
    }

    private String construirContexto(Usuario usuario, Objetivo objetivo, List<PlanoTreino> historicoPlanos) {
        return promptBuilder.construirPromptPlano(usuario, objetivo, historicoPlanos);
    }

    private PlanoTreino parsearResposta(String json, Usuario usuario) {
        PlanoTreinoParser parser = new PlanoTreinoParser();
        return parser.parsear(json, usuario);
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário é obrigatório.");
        }
        if (!usuario.estaAptoParaTreinar()) {
            throw new BusinessRuleException("Usuário não está apto para gerar plano de treino.");
        }
    }

    private void aplicarRegraDosDezPorCento(PlanoTreino planoTreino) {
        if (planoTreino == null || planoTreino.getSessoes() == null || planoTreino.getSessoes().isEmpty()) {
            return;
        }

        TreeSet<Integer> semanas = planoTreino.getSessoes().stream()
                .map(SessaoTreino::getNumeroSemana)
                .filter(Objects::nonNull)
                .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);

        Integer semanaAnterior = null;
        for (Integer semanaAtual : semanas) {
            if (semanaAnterior == null) {
                semanaAnterior = semanaAtual;
                continue;
            }

            BigDecimal volumeAnterior = calcularVolumeSemanal(planoTreino, semanaAnterior);
            BigDecimal volumeAtual = calcularVolumeSemanal(planoTreino, semanaAtual);
            if (volumeAnterior.signum() > 0) {
                BigDecimal limiteAceito = volumeAnterior.multiply(LIMITE_REGRA_DEZ_POR_CENTO);
                if (volumeAtual.compareTo(limiteAceito) > 0) {
                    ajustarVolumeSemanal(planoTreino, semanaAtual, limiteAceito, volumeAtual);
                }
            }
            semanaAnterior = semanaAtual;
        }

        recalcularKmTotais(planoTreino);
        ordenarSessoes(planoTreino);
        preencherDatasDerivadas(planoTreino);
    }

    private BigDecimal calcularVolumeSemanal(PlanoTreino planoTreino, Integer semana) {
        return planoTreino.obterSessoesDaSemana(semana).stream()
                .map(SessaoTreino::getDistanciaKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ajustarVolumeSemanal(PlanoTreino planoTreino,
                                      Integer semana,
                                      BigDecimal limiteAceito,
                                      BigDecimal volumeAtual) {
        if (volumeAtual.signum() <= 0) {
            return;
        }

        BigDecimal fatorReducao = limiteAceito.divide(volumeAtual, 8, RoundingMode.HALF_UP);
        for (SessaoTreino sessaoTreino : planoTreino.obterSessoesDaSemana(semana)) {
            if (sessaoTreino.getDistanciaKm() == null) {
                continue;
            }
            BigDecimal distanciaAjustada = sessaoTreino.getDistanciaKm()
                    .multiply(fatorReducao)
                    .setScale(SCALE_DISTANCIA, RoundingMode.HALF_UP);
            sessaoTreino.setDistanciaKm(distanciaAjustada);
        }
    }

    private void recalcularKmTotais(PlanoTreino planoTreino) {
        BigDecimal kmTotais = planoTreino.getSessoes().stream()
                .map(SessaoTreino::getDistanciaKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        planoTreino.setKmTotais(kmTotais);
    }

    private void ordenarSessoes(PlanoTreino planoTreino) {
        planoTreino.getSessoes().sort(Comparator
                .comparing(SessaoTreino::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SessaoTreino::getDataPrevista, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private void preencherDatasDerivadas(PlanoTreino planoTreino) {
        List<LocalDate> datas = planoTreino.getSessoes().stream()
                .map(SessaoTreino::getDataPrevista)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (planoTreino.getDataInicio() == null && !datas.isEmpty()) {
            planoTreino.setDataInicio(datas.get(0));
        }
        if (planoTreino.getDataFim() == null && !datas.isEmpty()) {
            planoTreino.setDataFim(datas.get(datas.size() - 1));
        }
    }

    private void herdarMetadadosBasicos(PlanoTreino planoOriginal, PlanoTreino planoReescrito) {
        if (planoReescrito.getUsuario() == null) {
            planoReescrito.setUsuario(planoOriginal.getUsuario());
        }
        if (planoReescrito.getResumoIA() == null || planoReescrito.getResumoIA().isBlank()) {
            planoReescrito.setResumoIA(planoOriginal.getResumoIA());
        }
        if (planoReescrito.getGuiaTiposTreino() == null || planoReescrito.getGuiaTiposTreino().isBlank()) {
            planoReescrito.setGuiaTiposTreino(planoOriginal.getGuiaTiposTreino());
        }
    }
}
