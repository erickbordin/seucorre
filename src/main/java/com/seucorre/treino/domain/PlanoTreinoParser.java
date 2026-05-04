package com.seucorre.treino.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.usuario.domain.Usuario;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class PlanoTreinoParser {

    private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    public PlanoTreinoParser() {
        this(new ObjectMapper());
    }

    public PlanoTreinoParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public PlanoTreino parsear(String jsonBruto) {
        return parsear(jsonBruto, null);
    }

    public PlanoTreino parsear(String jsonBruto, Usuario usuario) {
        JsonNode raiz = lerRaiz(jsonBruto);
        JsonNode planoNode = localizarPlanoNode(raiz);

        PlanoTreino planoTreino = new PlanoTreino();
        planoTreino.setUsuario(usuario);
        planoTreino.setTotalSemanas(inteiroOuNulo(planoNode, "totalSemanas", "total_semanas"));
        planoTreino.setDataInicio(dataOuNulo(planoNode, "dataInicio", "data_inicio"));
        planoTreino.setDataFim(dataOuNulo(planoNode, "dataFim", "data_fim"));
        planoTreino.setKmTotais(decimalOuNulo(planoNode, "kmTotais", "km_totais"));
        planoTreino.setResumoIA(textoOuNulo(planoNode, "resumoIA", "resumo_ia", "resumo", "racional"));
        planoTreino.setGuiaTiposTreino(textoOuNulo(planoNode, "guiaTiposTreino", "guia_tipos_treino"));

        adicionarSessoesDoPlano(planoTreino, planoNode);
        complementarCamposDerivados(planoTreino);
        validarResultado(planoTreino);

        return planoTreino;
    }

    private JsonNode lerRaiz(String jsonBruto) {
        if (jsonBruto == null || jsonBruto.isBlank()) {
            throw new BusinessRuleException("JSON bruto da IA é obrigatório.");
        }

        String jsonNormalizado = extrairJson(jsonBruto);
        try {
            JsonNode raiz = objectMapper.readTree(jsonNormalizado);
            if (raiz == null || raiz.isNull()) {
                throw new BusinessRuleException("A resposta da IA não contém um JSON válido.");
            }
            return raiz;
        } catch (IOException exception) {
            throw new BusinessRuleException("Falha ao interpretar o JSON retornado pela IA.");
        }
    }

    private JsonNode localizarPlanoNode(JsonNode raiz) {
        if (raiz.isObject() && raiz.has("plano") && raiz.get("plano").isObject()) {
            return raiz.get("plano");
        }
        if (!raiz.isObject()) {
            throw new BusinessRuleException("A resposta da IA deve ser um objeto JSON.");
        }
        return raiz;
    }

    private void adicionarSessoesDoPlano(PlanoTreino planoTreino, JsonNode planoNode) {
        List<SessaoTreino> sessoes = new ArrayList<>();

        JsonNode semanasNode = localizarCampo(planoNode, "semanas");
        if (semanasNode != null && semanasNode.isArray()) {
            for (JsonNode semanaNode : semanasNode) {
                Integer numeroSemana = inteiroOuNulo(semanaNode, "numeroSemana", "numero_semana", "semana");
                JsonNode sessoesNode = localizarCampo(semanaNode, "sessoes", "treinos");
                if (sessoesNode == null || !sessoesNode.isArray()) {
                    continue;
                }
                for (JsonNode sessaoNode : sessoesNode) {
                    sessoes.add(criarSessao(sessaoNode, numeroSemana));
                }
            }
        }

        JsonNode sessoesNode = localizarCampo(planoNode, "sessoes", "treinos");
        if ((semanasNode == null || semanasNode.isEmpty()) && sessoesNode != null && sessoesNode.isArray()) {
            for (JsonNode sessaoNode : sessoesNode) {
                sessoes.add(criarSessao(sessaoNode,
                        inteiroOuNulo(sessaoNode, "numeroSemana", "numero_semana", "semana")));
            }
        }

        for (SessaoTreino sessaoTreino : sessoes) {
            planoTreino.adicionarSessao(sessaoTreino);
        }
    }

    private SessaoTreino criarSessao(JsonNode sessaoNode, Integer numeroSemanaPadrao) {
        if (sessaoNode == null || !sessaoNode.isObject()) {
            throw new BusinessRuleException("Cada sessão do plano deve ser um objeto JSON.");
        }

        SessaoTreino sessaoTreino = new SessaoTreino();
        sessaoTreino.setNumeroSemana(primeiroNaoNulo(
                inteiroOuNulo(sessaoNode, "numeroSemana", "numero_semana", "semana"),
                numeroSemanaPadrao
        ));
        sessaoTreino.setDataPrevista(dataOuNulo(sessaoNode, "dataPrevista", "data_prevista", "data"));
        sessaoTreino.setTipo(tipoTreinoOuNulo(sessaoNode));
        sessaoTreino.setDistanciaKm(decimalOuNulo(sessaoNode, "distanciaKm", "distancia_km", "distancia"));
        sessaoTreino.setDuracaoMinutos(inteiroOuNulo(sessaoNode, "duracaoMinutos", "duracao_min", "duracao"));
        sessaoTreino.setIntensidade(textoOuNulo(sessaoNode, "intensidade"));
        sessaoTreino.setZonaFcAlvo(textoOuNulo(sessaoNode, "zonaFcAlvo", "zona_fc_alvo", "zonaFc", "zona_fc"));
        sessaoTreino.setPaceAlvo(decimalOuNulo(sessaoNode, "paceAlvo", "pace_alvo", "pace"));
        sessaoTreino.setDescricao(textoOuNulo(sessaoNode, "descricao", "descrição"));

        return sessaoTreino;
    }

    private void complementarCamposDerivados(PlanoTreino planoTreino) {
        if (planoTreino.getTotalSemanas() == null) {
            planoTreino.setTotalSemanas(planoTreino.getSessoes().stream()
                    .map(SessaoTreino::getNumeroSemana)
                    .filter(numeroSemana -> numeroSemana != null && numeroSemana > 0)
                    .max(Integer::compareTo)
                    .orElse(null));
        }

        if (planoTreino.getDataInicio() == null) {
            planoTreino.setDataInicio(planoTreino.getSessoes().stream()
                    .map(SessaoTreino::getDataPrevista)
                    .filter(data -> data != null)
                    .min(LocalDate::compareTo)
                    .orElse(null));
        }

        if (planoTreino.getDataFim() == null) {
            planoTreino.setDataFim(planoTreino.getSessoes().stream()
                    .map(SessaoTreino::getDataPrevista)
                    .filter(data -> data != null)
                    .max(LocalDate::compareTo)
                    .orElse(null));
        }

        if (planoTreino.getKmTotais() == null) {
            BigDecimal kmTotais = planoTreino.getSessoes().stream()
                    .map(SessaoTreino::getDistanciaKm)
                    .filter(distancia -> distancia != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            planoTreino.setKmTotais(kmTotais.signum() == 0 ? null : kmTotais);
        }
    }

    private void validarResultado(PlanoTreino planoTreino) {
        if (planoTreino.getSessoes() == null || planoTreino.getSessoes().isEmpty()) {
            throw new BusinessRuleException("A resposta da IA não contém sessões de treino.");
        }

        boolean sessaoSemTipo = planoTreino.getSessoes().stream().anyMatch(sessao -> sessao.getTipo() == null);
        if (sessaoSemTipo) {
            throw new BusinessRuleException("A resposta da IA contém sessão sem tipo de treino válido.");
        }
    }

    private TipoTreino tipoTreinoOuNulo(JsonNode node) {
        String valorBruto = textoOuNulo(node, "tipo", "tipoTreino", "tipo_treino");
        if (valorBruto == null) {
            return null;
        }

        String valorNormalizado = normalizar(valorBruto);
        if (valorNormalizado.contains("INTERVAL")) {
            return TipoTreino.INTERVALADO;
        }
        if (valorNormalizado.contains("FARTLEK")
                || valorNormalizado.contains("FARTLAKE")
                || valorNormalizado.contains("FARTLAK")) {
            return TipoTreino.FARTLAKE;
        }
        if (valorNormalizado.contains("LONG")) {
            return TipoTreino.LONGO;
        }
        if (valorNormalizado.contains("REGENERAT")
                || valorNormalizado.contains("RECUPER")
                || valorNormalizado.contains("RODAGEM")
                || valorNormalizado.contains("LEVE")) {
            return TipoTreino.REGENERATIVO;
        }

        try {
            return TipoTreino.valueOf(valorNormalizado);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Integer inteiroOuNulo(JsonNode node, String... nomesCampos) {
        JsonNode campo = localizarCampo(node, nomesCampos);
        if (campo == null || campo.isNull()) {
            return null;
        }
        if (campo.isInt() || campo.isLong()) {
            return campo.intValue();
        }
        if (campo.isTextual()) {
            try {
                return Integer.parseInt(campo.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal decimalOuNulo(JsonNode node, String... nomesCampos) {
        JsonNode campo = localizarCampo(node, nomesCampos);
        if (campo == null || campo.isNull()) {
            return null;
        }
        if (campo.isNumber()) {
            return campo.decimalValue();
        }
        if (campo.isTextual()) {
            String valor = campo.asText().trim().replace(",", ".");
            if (valor.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private LocalDate dataOuNulo(JsonNode node, String... nomesCampos) {
        JsonNode campo = localizarCampo(node, nomesCampos);
        if (campo == null || campo.isNull() || !campo.isTextual()) {
            return null;
        }

        String valor = campo.asText().trim();
        if (valor.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(valor, FORMATTER_BR);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private String textoOuNulo(JsonNode node, String... nomesCampos) {
        JsonNode campo = localizarCampo(node, nomesCampos);
        if (campo == null || campo.isNull()) {
            return null;
        }
        String texto = campo.asText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private JsonNode localizarCampo(JsonNode node, String... nomesCampos) {
        if (node == null || !node.isObject()) {
            return null;
        }

        for (String nomeCampo : nomesCampos) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (normalizar(fieldName).equals(normalizar(nomeCampo))) {
                    return node.get(fieldName);
                }
            }
        }
        return null;
    }

    private String extrairJson(String jsonBruto) {
        String texto = jsonBruto.trim();
        if (texto.startsWith("```")) {
            texto = removerCercasMarkdown(texto);
        }

        int inicioObjeto = texto.indexOf('{');
        int fimObjeto = texto.lastIndexOf('}');
        if (inicioObjeto >= 0 && fimObjeto > inicioObjeto) {
            return texto.substring(inicioObjeto, fimObjeto + 1);
        }
        return texto;
    }

    private String removerCercasMarkdown(String texto) {
        String semAbertura = texto.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return semAbertura.replaceFirst("\\s*```\\s*$", "").trim();
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT)
                .trim();
    }

    private Integer primeiroNaoNulo(Integer valor, Integer valorPadrao) {
        return valor != null ? valor : valorPadrao;
    }
}
