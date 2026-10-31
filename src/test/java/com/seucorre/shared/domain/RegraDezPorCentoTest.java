package com.seucorre.shared.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegraDezPorCentoTest {

    @ParameterizedTest
    @CsvSource({
            "10, 11, false",
            "10, 11.01, true",
            "10, 20, true",
            "10, 5, false",
            "0, 50, false",
            "-3, 50, false"
    })
    void excedeQuandoOVolumePassaDe110PorCentoDoAnterior(String anterior, String atual, boolean esperado) {
        assertThat(RegraDezPorCento.excede(new BigDecimal(anterior), new BigDecimal(atual))).isEqualTo(esperado);
    }
}
