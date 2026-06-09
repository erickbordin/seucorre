package com.seucorre.shared.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;

@Getter
@EqualsAndHashCode
public class PaceAlvo {

    private final int minutos;
    private final int segundos;
    private final int totalSegundos;

    private PaceAlvo(int minutos, int segundos) {
        this.minutos = minutos;
        this.segundos = segundos;
        this.totalSegundos = (minutos * 60) + segundos;
    }

    /**
     * Factory Method para criar um Pace.
     * Ex: PaceAlvo.of(5, 30) para 5:30/km
     */
    public static PaceAlvo of(int minutos, int segundos) {
        if (minutos < 0 || segundos < 0 || segundos > 59) {
            throw new IllegalArgumentException("Pace inválido: minutos >= 0 e segundos entre 0 e 59.");
        }
        if (minutos == 0 && segundos == 0) {
            throw new IllegalArgumentException("O Pace não pode ser zero.");
        }
        return new PaceAlvo(minutos, segundos);
    }

    /**
     * Verifica se um pace real está entre um limite mais rápido e um mais lento.
     * Importante: Na corrida, o pace MAIS RÁPIDO tem MENOS segundos totais.
     */
    public boolean estaNoIntervalo(PaceAlvo maisRapido, PaceAlvo maisLento) {
        return this.totalSegundos >= maisRapido.getTotalSegundos() && 
               this.totalSegundos <= maisLento.getTotalSegundos();
    }

    /**
     * Calcula o tempo total estimado para percorrer uma distância.
     */
    public Duration calcularTempoProva(double distanciaKm) {
        if (distanciaKm <= 0) {
            throw new IllegalArgumentException("A distância deve ser positiva.");
        }
        long segundosTotais = Math.round(this.totalSegundos * distanciaKm);
        return Duration.ofSeconds(segundosTotais);
    }

    @Override
    public String toString() {
        return String.format("%d:%02d/km", minutos, segundos);
    }
}