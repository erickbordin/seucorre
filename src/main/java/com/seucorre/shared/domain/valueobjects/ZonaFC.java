package com.seucorre.shared.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ZonaFC {

    private final int numero;
    private final int minBpm;
    private final int maxBpm;

    private ZonaFC(int numero, int minBpm, int maxBpm) {
        this.numero = numero;
        this.minBpm = minBpm;
        this.maxBpm = maxBpm;
    }

    /**
     * Factory Method para criar uma zona baseada nas porcentagens padrão de FC Máx.
     * Z1: 50-60%, Z2: 60-70%, Z3: 70-80%, Z4: 80-90%, Z5: 90-100%
     */
    public static ZonaFC criarZonaPadrao(int numero, int fcMax) {
        if (fcMax <= 0) throw new IllegalArgumentException("FC Máxima deve ser positiva.");
        
        int min, max;
        switch (numero) {
            case 1 -> { min = (int) (fcMax * 0.50); max = (int) (fcMax * 0.60); }
            case 2 -> { min = (int) (fcMax * 0.60); max = (int) (fcMax * 0.70); }
            case 3 -> { min = (int) (fcMax * 0.70); max = (int) (fcMax * 0.80); }
            case 4 -> { min = (int) (fcMax * 0.80); max = (int) (fcMax * 0.90); }
            case 5 -> { min = (int) (fcMax * 0.90); max = fcMax; }
            default -> throw new IllegalArgumentException("Zona deve ser entre 1 e 5.");
        }
        
        return new ZonaFC(numero, min, max);
    }

    public static ZonaFC of(int numero, int minBpm, int maxBpm) {
        if (numero <= 0) {
            throw new IllegalArgumentException("Zona deve ser positiva.");
        }
        if (minBpm <= 0 || maxBpm <= 0) {
            throw new IllegalArgumentException("Frequências da zona devem ser positivas.");
        }
        if (maxBpm < minBpm) {
            throw new IllegalArgumentException("FC máxima da zona deve ser maior ou igual à FC mínima.");
        }
        return new ZonaFC(numero, minBpm, maxBpm);
    }

    public boolean contemFrequencia(int fc) {
        if (numero == 5) {
            return fc >= minBpm && fc <= maxBpm;
        }
        return fc >= minBpm && fc < maxBpm;
    }

    public int getAmplitude() {
        return maxBpm - minBpm;
    }
}
