package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.RegraDezPorCento;
import com.seucorre.shared.domain.enums.NivelRisco;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.usuario.domain.Usuario;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Service
public class GeradorPlanoIA {

    private static final int SCALE_DISTANCIA = 2;
    private static final BigDecimal FATOR_MAXIMO_RISCO_CRITICO = new BigDecimal("0.70");

    private final IAClient iaClient;
    private final PromptBuilder promptBuilder;
    private final AgendaDeTreino agendaDeTreino;

    public GeradorPlanoIA(IAClient iaClient, PromptBuilder promptBuilder, Clock clock) {
        if (iaClient == null) {
            throw new IllegalArgumentException("IAClient é obrigatório.");
        }
        if (promptBuilder == null) {
            throw new IllegalArgumentException("PromptBuilder é obrigatório.");
        }
        this.iaClient = iaClient;
        this.promptBuilder = promptBuilder;
        this.agendaDeTreino = new AgendaDeTreino(clock);
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
        aplicarRegraDosDezPorCento(planoTreino, null);
        agendaDeTreino.agendar(planoTreino, usuario.getDiasSemanaTreino());
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
        BigDecimal volumeDeReferencia = plano.calcularVolumeSemanal(checkin.getSemana());
        if (checkin.avaliarNivelRisco() == NivelRisco.CRITICO) {
            aplicarRegraDeRiscoCritico(planoReescrito, volumeDeReferencia);
        }
        aplicarRegraDosDezPorCento(planoReescrito, volumeDeReferencia);
        LocalDate ultimoTreinoDaSemana = plano.obterSessoesDaSemana(checkin.getSemana()).stream()
                .map(SessaoTreino::getDataPrevista)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        agendaDeTreino.agendarDepoisDe(planoReescrito, planoReescrito.getUsuario().getDiasSemanaTreino(), ultimoTreinoDaSemana);
        checkin.setPlanoReescrito(true);
        return planoReescrito;
    }

    public String gerarAnaliseCheckin(CheckinSemanal checkin, boolean planoSeraAjustado) {
        if (checkin == null) {
            throw new IllegalArgumentException("Check-in é obrigatório.");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce escreve a leitura semanal do check-in para o corredor no app SeuCorre.\n");
        prompt.append("Dados informados pelo corredor:\n");
        prompt.append("- esforco percebido: ").append(valorOuNaoInformado(checkin.getNivelEsforco())).append(" de 10\n");
        prompt.append("- dor: ").append(valorOuNaoInformado(checkin.getNivelDor())).append(" de 10\n");
        prompt.append("- sono medio: ").append(valorOuNaoInformado(checkin.getHorasSonoSemana())).append(" horas por noite\n");
        if (checkin.getAvaliacao() != null && !checkin.getAvaliacao().isBlank()) {
            prompt.append("- comentario: ").append(checkin.getAvaliacao()).append('\n');
        }
        prompt.append("Decisao ja tomada pelo sistema, que voce explica e nunca contradiz:\n");
        prompt.append("- ").append(checkin.avaliarNivelRisco().getDescricao()).append('\n');
        prompt.append(planoSeraAjustado
                ? "- as proximas semanas do plano serao ajustadas para reduzir a carga\n"
                : "- o plano segue como esta\n");
        prompt.append("Tarefa:\n");
        prompt.append("- escrever de 2 a 3 frases curtas em portugues, falando diretamente com o corredor\n");
        prompt.append("- dizer o que pesou na decisao (dor, sono ou esforco) e um cuidado pratico para os proximos dias\n");
        prompt.append("- nao sugerir distancias, paces nem quantidade de treinos: o plano ja define isso\n");
        prompt.append("- responder em texto puro, sem markdown, listas, titulos, valores true ou false e nomes de campos\n");
        return iaClient.gerarResposta(prompt.toString());
    }

    private static String valorOuNaoInformado(Integer valor) {
        return valor == null ? "nao informado" : valor.toString();
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

    private void aplicarRegraDosDezPorCento(PlanoTreino planoTreino, BigDecimal volumeDeReferencia) {
        if (planoTreino == null || planoTreino.getSessoes() == null || planoTreino.getSessoes().isEmpty()) {
            return;
        }

        TreeSet<Integer> semanas = planoTreino.getSessoes().stream()
                .map(SessaoTreino::getNumeroSemana)
                .filter(Objects::nonNull)
                .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);

        BigDecimal volumeAnterior = volumeDeReferencia;
        for (Integer semanaAtual : semanas) {
            BigDecimal volumeAtual = planoTreino.calcularVolumeSemanal(semanaAtual);
            if (RegraDezPorCento.excede(volumeAnterior, volumeAtual)) {
                ajustarVolumeSemanal(planoTreino, semanaAtual, RegraDezPorCento.limite(volumeAnterior), volumeAtual);
            }
            volumeAnterior = planoTreino.calcularVolumeSemanal(semanaAtual);
        }
    }

    private void ajustarVolumeSemanal(PlanoTreino planoTreino,
                                      Integer semana,
                                      BigDecimal limiteAceito,
                                      BigDecimal volumeAtual) {
        if (volumeAtual.signum() <= 0) {
            return;
        }

        for (SessaoTreino sessaoTreino : planoTreino.obterSessoesDaSemana(semana)) {
            if (sessaoTreino.getDistanciaKm() == null) {
                continue;
            }
            BigDecimal distanciaAjustada = sessaoTreino.getDistanciaKm()
                    .multiply(limiteAceito)
                    .divide(volumeAtual, SCALE_DISTANCIA, RoundingMode.DOWN);
            sessaoTreino.setDistanciaKm(distanciaAjustada);
        }
    }

    private void aplicarRegraDeRiscoCritico(PlanoTreino planoTreino, BigDecimal volumeDeReferencia) {
        Integer primeiraSemana = planoTreino.getSessoes().stream()
                .map(SessaoTreino::getNumeroSemana)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
        if (primeiraSemana == null) {
            return;
        }

        for (SessaoTreino sessaoTreino : planoTreino.obterSessoesDaSemana(primeiraSemana)) {
            if (sessaoTreino.getTipo() == TipoTreino.INTERVALADO) {
                sessaoTreino.setTipo(TipoTreino.REGENERATIVO);
                sessaoTreino.setIntensidade("leve");
                sessaoTreino.setPaceAlvo(null);
                sessaoTreino.setDescricao("Rodagem leve no lugar do intervalado: o check-in indicou risco crítico de lesão.");
            }
        }

        BigDecimal limite = volumeDeReferencia.multiply(FATOR_MAXIMO_RISCO_CRITICO);
        BigDecimal volumeAtual = planoTreino.calcularVolumeSemanal(primeiraSemana);
        if (limite.signum() > 0 && volumeAtual.compareTo(limite) > 0) {
            ajustarVolumeSemanal(planoTreino, primeiraSemana, limite, volumeAtual);
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
