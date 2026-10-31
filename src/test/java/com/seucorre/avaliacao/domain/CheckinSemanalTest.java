package com.seucorre.avaliacao.domain;

import com.seucorre.shared.domain.enums.NivelRisco;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CheckinSemanalTest {

    @ParameterizedTest(name = "esforço {0}, dor {1}, sono {2} -> {3}")
    @CsvSource({
            "5, 0, 8, BAIXO",
            "5, 3, 7, BAIXO",
            "8, 0, 7, BAIXO",
            "5, 4, 7, MODERADO",
            "5, 0, 6, MODERADO",
            "9, 0, 8, MODERADO",
            "8, 0, 4, ALTO",
            "5, 7, 7, ALTO",
            "5, 8, 5, ALTO",
            "5, 7, 4, CRITICO",
            "5, 9, 8, CRITICO",
            " ,  ,  , BAIXO"
    })
    void classificaORiscoNosLimitesDeEsforcoDorESono(Integer esforco, Integer dor, Integer sono, NivelRisco esperado) {
        CheckinSemanal checkin = new CheckinSemanal();
        checkin.setNivelEsforco(esforco);
        checkin.setNivelDor(dor);
        checkin.setHorasSonoSemana(sono);

        assertThat(checkin.avaliarNivelRisco()).isEqualTo(esperado);
    }
}
