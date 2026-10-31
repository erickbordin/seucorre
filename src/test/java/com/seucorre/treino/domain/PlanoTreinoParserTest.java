package com.seucorre.treino.domain;

import com.seucorre.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanoTreinoParserTest {

    private final PlanoTreinoParser parser = new PlanoTreinoParser();

    @Test
    void parseiaJsonEnvolvidoPorTextoEMarkdown() {
        String respostaIa = """
                Aqui esta o plano solicitado.
                ```json
                {
                  "plano": {
                    "totalSemanas": 1,
                    "dataInicio": "2026-05-12",
                    "dataFim": "2026-05-18",
                    "kmTotais": 10.0,
                    "resumoIA": "Plano inicial.",
                    "guiaTiposTreino": "Leve e progressivo.",
                    "semanas": [
                      {
                        "numeroSemana": 1,
                        "sessoes": [
                          {
                            "dataPrevista": "2026-05-12",
                            "tipo": "REGENERATIVO",
                            "distanciaKm": 5.0,
                            "duracaoMinutos": 35,
                            "intensidade": "leve",
                            "zonaFcAlvo": "Z1",
                            "paceAlvo": 6.5,
                            "descricao": "Rodagem leve"
                          },
                          {
                            "dataPrevista": "2026-05-15",
                            "tipo": "LONGO",
                            "distanciaKm": 5.0,
                            "duracaoMinutos": 38,
                            "intensidade": "leve",
                            "zonaFcAlvo": "Z2",
                            "paceAlvo": 6.4,
                            "descricao": "Longo curto"
                          }
                        ]
                      }
                    ]
                  }
                }
                ```
                Observacao final que deve ser ignorada.
                """;

        PlanoTreino planoTreino = parser.parsear(respostaIa);

        assertThat(planoTreino.getTotalSemanas()).isEqualTo(1);
        assertThat(planoTreino.getSessoes()).hasSize(2);
        assertThat(planoTreino.getResumoIA()).isEqualTo("Plano inicial.");
    }

    @Test
    void parseiaPrimeiroObjetoJsonBalanceadoMesmoComTextoDepois() {
        String respostaIa = """
                {
                  "plano": {
                    "totalSemanas": 1,
                    "dataInicio": "2026-05-12",
                    "dataFim": "2026-05-12",
                    "kmTotais": 4.0,
                    "resumoIA": "Sessao unica.",
                    "guiaTiposTreino": "Recuperacao.",
                    "sessoes": [
                      {
                        "numeroSemana": 1,
                        "dataPrevista": "2026-05-12",
                        "tipo": "REGENERATIVO",
                        "distanciaKm": 4.0,
                        "duracaoMinutos": 28,
                        "intensidade": "leve",
                        "zonaFcAlvo": "Z1",
                        "paceAlvo": 6.7,
                        "descricao": "Rodagem"
                      }
                    ]
                  }
                }
                Texto extra que deve ser descartado.
                """;

        PlanoTreino planoTreino = parser.parsear(respostaIa);

        assertThat(planoTreino.getSessoes()).singleElement();
        assertThat(planoTreino.getKmTotais()).isNotNull();
    }

    @Test
    void recusaJsonTruncado() {
        String respostaIa = """
                {
                  "plano": {
                    "resumoIA": "Plano enxuto.",
                    "guiaTiposTreino": "Base e longo.",
                    "sessoes": [
                      {
                        "numeroSemana": 1,
                        "dataPrevista": "2026-05-12",
                        "tipo": "velocidade",
                        "distanciaKm": 4.0,
                        "duracaoMinutos": 28,
                        "intensidade": "moderada",
                        "zonaFcAlvo": "Z3",
                        "paceAlvo": 5.4,
                        "descricao": "Tiros curtos"
                      },
                      {
                        "numeroSemana": 1,
                        "dataPrevista": "2026-05-15",
                        "tipo": "distancia",
                        "distanciaKm": 8.0,
                        "duracaoMinutos": 52,
                        "intensidade": "leve",
                        "zonaFcAlvo": "Z2",
                        "paceAlvo": 6.1,
                        "descricao": "Rodagem longa"
                      }
                """;

        assertThatThrownBy(() -> parser.parsear(respostaIa))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Falha ao interpretar o JSON retornado pela IA.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"sessoes\": [{\"tipo\": \"longo\", \"distanciaKm\": 5}]} | A resposta da IA contém sessão sem número de semana.",
            "{\"sessoes\": [{\"numeroSemana\": 1, \"tipo\": \"longo\", \"distanciaKm\": -3}]} | A resposta da IA contém sessão com distância menor ou igual a zero.",
            "{\"totalSemanas\": 12, \"sessoes\": [{\"numeroSemana\": 1, \"tipo\": \"longo\", \"distanciaKm\": 5}]} | A resposta da IA não trouxe todas as semanas do plano."
    })
    void recusaPlanoComSessaoInvalidaOuSemanasFaltando(String respostaIa, String mensagem) {
        assertThatThrownBy(() -> parser.parsear(respostaIa))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(mensagem);
    }
}
