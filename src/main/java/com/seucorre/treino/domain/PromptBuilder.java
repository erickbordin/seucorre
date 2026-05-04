package com.seucorre.treino.domain;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.avaliacao.domain.ProgressoSemanal;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.usuario.domain.PerfilAtleta;
import com.seucorre.usuario.domain.PerfilCorrida;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.domain.ZonaFCPersistida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PromptBuilder {

    private static final int LIMITE_PLANOS_HISTORICO = 3;
    private static final int LIMITE_SESSOES_RESUMO = 6;
    private static final String GUIA_TIPOS_TREINO = String.join("\n",
            "- regenerativo: baixa intensidade para recuperacao ativa",
            "- rodagem leve: base aerobica em zona confortavel",
            "- longo: desenvolvimento de resistencia com controle de carga",
            "- ritmo/tempo: sustentacao de esforco proximo ao limiar",
            "- intervalado: blocos intensos com recuperacao definida",
            "- fortalecimento ou descanso: suporte a prevencao de lesao e recuperacao");

    public String construirPromptPlano(Usuario usuario,
                                       Objetivo objetivo,
                                       List<PlanoTreino> historicoPlanos) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce e o motor de planejamento do SeuCorre.\n");
        prompt.append("Monte um plano de treino de corrida seguro, progressivo e coerente com o corredor.\n\n");

        adicionarContextoCorredor(prompt, usuario, objetivo);
        adicionarHistoricoPlanos(prompt, historicoPlanos);
        adicionarRegrasTreino(prompt, null);
        adicionarInstrucaoSaidaPlano(prompt);

        return prompt.toString();
    }

    public String construirPromptReescrita(PlanoTreino plano,
                                           CheckinSemanal checkin,
                                           List<ProgressoSemanal> progressos) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce e o motor de reescrita adaptativa do SeuCorre.\n");
        prompt.append("Reescreva o plano atual priorizando seguranca, recuperacao e aderencia ao objetivo.\n\n");

        Usuario usuario = plano == null ? null : plano.getUsuario();
        adicionarContextoCorredor(prompt, usuario, objetivoOuDoUsuario(usuario));
        adicionarPlanoAtual(prompt, plano);
        adicionarHistoricoProgressos(prompt, progressos);
        adicionarCheckin(prompt, checkin);
        adicionarRegrasTreino(prompt, plano == null ? null : plano.getGuiaTiposTreino());
        adicionarInstrucaoSaidaReescrita(prompt);

        return prompt.toString();
    }

    private void adicionarContextoCorredor(StringBuilder prompt, Usuario usuario, Objetivo objetivo) {
        prompt.append("Contexto do corredor:\n");
        if (usuario == null) {
            prompt.append("- Usuario nao informado.\n\n");
            return;
        }

        prompt.append("- Nome: ").append(textoOuPadrao(usuario.getNome(), "nao informado")).append('\n');
        prompt.append("- Objetivo: ").append(descreverObjetivo(objetivo)).append('\n');
        prompt.append("- Resumo consolidado: ")
                .append(textoOuPadrao(usuario.gerarResumoParaIA(), "perfil nao informado"))
                .append('\n');
        prompt.append("- Dias disponiveis por semana: ")
                .append(valorOuNaoInformado(usuario.getDiasDisponiveisSemana()))
                .append('\n');
        prompt.append("- Dias preferenciais: ")
                .append(textoOuPadrao(usuario.getDiasSemanaTreino(), "nao informados"))
                .append('\n');
        prompt.append("- FC max disponivel ou teorica: ").append(usuario.calcularFcMaxTeorica()).append('\n');
        prompt.append("- Apto para treinar: ").append(usuario.estaAptoParaTreinar()).append('\n');
        prompt.append("- Zonas de FC: ").append(resumirZonasFc(usuario)).append("\n\n");
    }

    private void adicionarHistoricoPlanos(StringBuilder prompt, List<PlanoTreino> historicoPlanos) {
        prompt.append("Historico de planos:\n");
        if (historicoPlanos == null || historicoPlanos.isEmpty()) {
            prompt.append("- Nenhum plano anterior disponivel.\n\n");
            return;
        }

        historicoPlanos.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PlanoTreino::getDataInicio, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMITE_PLANOS_HISTORICO)
                .forEach(plano -> {
                    prompt.append("- Inicio ").append(formatarData(plano.getDataInicio()))
                            .append(", fim ").append(formatarData(plano.getDataFim()))
                            .append(", status ").append(valorOuNaoInformado(plano.getStatus()))
                            .append(", totalSemanas ").append(valorOuNaoInformado(plano.getTotalSemanas()))
                            .append(", kmTotais ").append(formatarDecimal(plano.getKmTotais()))
                            .append(", progressoExecutado ")
                            .append(formatarPercentual(plano.calcularProgressoGeral()))
                            .append(".\n");
                    prompt.append("  Sessoes representativas: ")
                            .append(resumirSessoes(plano))
                            .append('\n');
                });
        prompt.append('\n');
    }

    private void adicionarPlanoAtual(StringBuilder prompt, PlanoTreino plano) {
        prompt.append("Plano atual:\n");
        if (plano == null) {
            prompt.append("- Plano nao informado.\n\n");
            return;
        }

        prompt.append("- Inicio ").append(formatarData(plano.getDataInicio()))
                .append(", fim ").append(formatarData(plano.getDataFim()))
                .append(", status ").append(valorOuNaoInformado(plano.getStatus()))
                .append(", totalSemanas ").append(valorOuNaoInformado(plano.getTotalSemanas()))
                .append(", kmTotais ").append(formatarDecimal(plano.getKmTotais()))
                .append(".\n");
        prompt.append("- Resumo IA atual: ")
                .append(textoOuPadrao(plano.getResumoIA(), "nao informado"))
                .append('\n');
        prompt.append("- Sessoes planejadas: ").append(resumirSessoes(plano)).append("\n\n");
    }

    private void adicionarHistoricoProgressos(StringBuilder prompt, List<ProgressoSemanal> progressos) {
        prompt.append("Historico de progresso:\n");
        if (progressos == null || progressos.isEmpty()) {
            prompt.append("- Nenhum progresso semanal disponivel.\n\n");
            return;
        }

        progressos.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProgressoSemanal::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(progresso -> prompt.append("- ").append(progresso.gerarResumo()).append('\n'));
        prompt.append('\n');
    }

    private void adicionarCheckin(StringBuilder prompt, CheckinSemanal checkin) {
        prompt.append("Check-in mais recente:\n");
        if (checkin == null) {
            prompt.append("- Nenhum check-in informado.\n\n");
            return;
        }

        prompt.append("- Contexto calculado: ").append(checkin.gerarContextoParaIA()).append('\n');
        prompt.append("- Precisa reescrever plano: ").append(checkin.precisaReescreverPlano()).append("\n\n");
    }

    private void adicionarRegrasTreino(StringBuilder prompt, String guiaTiposTreino) {
        prompt.append("Regras de treino e montagem:\n");
        prompt.append("- respeitar progressao gradual de carga e a regra dos 10 por cento\n");
        prompt.append("- distribuir sessoes conforme disponibilidade semanal e dias preferenciais\n");
        prompt.append("- usar zonas de FC e referencias de pace sempre que existirem\n");
        prompt.append("- priorizar seguranca, recuperacao e sinais de sobrecarga\n");
        prompt.append("- manter coerencia entre objetivo, nivel do corredor e historico recente\n");
        prompt.append("- guia de tipos de treino:\n");
        prompt.append(textoOuPadrao(guiaTiposTreino, GUIA_TIPOS_TREINO)).append("\n\n");
    }

    private void adicionarInstrucaoSaidaPlano(StringBuilder prompt) {
        prompt.append("Saida esperada:\n");
        prompt.append("- gerar um plano semanal completo e realista\n");
        prompt.append("- explicar de forma curta o racional principal\n");
        prompt.append("- responder em JSON estruturado para posterior parse em PlanoTreino e SessaoTreino\n");
    }

    private void adicionarInstrucaoSaidaReescrita(StringBuilder prompt) {
        prompt.append("Saida esperada:\n");
        prompt.append("- reescrever apenas o necessario para reduzir risco e preservar continuidade\n");
        prompt.append("- se houver alerta relevante, reduzir volume ou intensidade antes de progredir\n");
        prompt.append("- responder em JSON estruturado para posterior parse em PlanoTreino e SessaoTreino\n");
    }

    private String resumirSessoes(PlanoTreino plano) {
        if (plano == null || plano.getSessoes() == null || plano.getSessoes().isEmpty()) {
            return "nenhuma sessao cadastrada";
        }

        return plano.getSessoes().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SessaoTreino::getNumeroSemana, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SessaoTreino::getDataPrevista, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(LIMITE_SESSOES_RESUMO)
                .map(this::formatarSessao)
                .collect(Collectors.joining(" | "));
    }

    private String formatarSessao(SessaoTreino sessao) {
        String execucao = sessao.obterRegistro() == null
                ? "sem execucao"
                : sessao.obterRegistro().gerarResumoParaIA();
        return "semana " + valorOuNaoInformado(sessao.getNumeroSemana())
                + ", data " + formatarData(sessao.getDataPrevista())
                + ", tipo " + valorOuNaoInformado(sessao.getTipo())
                + ", distancia " + formatarDecimal(sessao.getDistanciaKm()) + " km"
                + ", duracao " + valorOuNaoInformado(sessao.getDuracaoMinutos()) + " min"
                + ", intensidade " + textoOuPadrao(sessao.getIntensidade(), "nao informada")
                + ", zonaFC " + textoOuPadrao(sessao.getZonaFcAlvo(), "nao informada")
                + ", paceAlvo " + formatarDecimal(sessao.getPaceAlvo())
                + ", execucao " + execucao;
    }

    private String resumirZonasFc(Usuario usuario) {
        PerfilCorrida perfilCorrida = resolverPerfilCorrida(usuario);
        if (perfilCorrida == null || perfilCorrida.getZonasFc() == null || perfilCorrida.getZonasFc().isEmpty()) {
            return "nao cadastradas";
        }

        return perfilCorrida.getZonasFc().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ZonaFCPersistida::getZona, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::formatarZona)
                .collect(Collectors.joining("; "));
    }

    private PerfilCorrida resolverPerfilCorrida(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        PerfilAtleta perfilAtleta = usuario.getPerfilAtleta();
        return perfilAtleta == null ? null : perfilAtleta.getPerfilCorrida();
    }

    private Objetivo objetivoOuDoUsuario(Usuario usuario) {
        return usuario == null ? null : usuario.getObjetivo();
    }

    private String descreverObjetivo(Objetivo objetivo) {
        return objetivo == null ? "nao informado" : objetivo.getDescricao();
    }

    private String formatarZona(ZonaFCPersistida zona) {
        String nome = textoOuPadrao(zona.getNome(), "Zona " + valorOuNaoInformado(zona.getZona()));
        return nome + " [" + valorOuNaoInformado(zona.getFcMin()) + "-" + valorOuNaoInformado(zona.getFcMax()) + " bpm]";
    }

    private String formatarDecimal(BigDecimal valor) {
        if (valor == null) {
            return "nao informado";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatarPercentual(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String formatarData(LocalDate data) {
        return data == null ? "nao informada" : data.toString();
    }

    private String valorOuNaoInformado(Object valor) {
        return valor == null ? "nao informado" : valor.toString();
    }

    private String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto.trim();
    }
}
