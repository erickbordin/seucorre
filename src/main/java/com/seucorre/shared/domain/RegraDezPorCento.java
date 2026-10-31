package com.seucorre.shared.domain;

import java.math.BigDecimal;

public final class RegraDezPorCento {

    private static final BigDecimal FATOR_MAXIMO = new BigDecimal("1.10");

    private RegraDezPorCento() {
    }

    public static BigDecimal limite(BigDecimal volumeAnterior) {
        return volumeAnterior.multiply(FATOR_MAXIMO);
    }

    public static boolean excede(BigDecimal volumeAnterior, BigDecimal volumeAtual) {
        if (volumeAnterior == null || volumeAtual == null || volumeAnterior.signum() <= 0) {
            return false;
        }
        return volumeAtual.compareTo(limite(volumeAnterior)) > 0;
    }
}
