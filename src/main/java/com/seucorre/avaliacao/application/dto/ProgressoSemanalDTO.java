package com.seucorre.avaliacao.application.dto;

import com.seucorre.avaliacao.domain.ProgressoSemanal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressoSemanalDTO(
        UUID id,
        UUID planoId,
        Integer numeroSemana,
        LocalDate dataInicioSemana,
        BigDecimal volumeKm,
        BigDecimal paceMedio,
        Integer fcRepousoMedio,
        Integer treinosConcluidos,
        Integer treinosParciais,
        Integer treinosPerdidos,
        int totalTreinos,
        double taxaAdesao,
        String resumo
) {

    public static ProgressoSemanalDTO from(ProgressoSemanal progressoSemanal) {
        if (progressoSemanal == null) {
            return null;
        }

        return new ProgressoSemanalDTO(
                progressoSemanal.getId(),
                progressoSemanal.getPlano() == null ? null : progressoSemanal.getPlano().getId(),
                progressoSemanal.getNumeroSemana(),
                progressoSemanal.getDataInicioSemana(),
                progressoSemanal.getVolumeKm(),
                progressoSemanal.getPaceMedio(),
                progressoSemanal.getFcRepousoMedio(),
                progressoSemanal.getTreinosConcluidos(),
                progressoSemanal.getTreinosParciais(),
                progressoSemanal.getTreinosPerdidos(),
                progressoSemanal.totalTreinos(),
                progressoSemanal.calcularTaxaAdesao(),
                progressoSemanal.gerarResumo()
        );
    }
}
