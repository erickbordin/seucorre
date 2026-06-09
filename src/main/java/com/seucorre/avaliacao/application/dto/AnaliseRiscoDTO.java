package com.seucorre.avaliacao.application.dto;

import com.seucorre.avaliacao.domain.CheckinSemanal;
import com.seucorre.shared.domain.enums.NivelRisco;

import java.time.LocalDate;
import java.util.UUID;

public record AnaliseRiscoDTO(
        UUID checkinId,
        UUID usuarioId,
        UUID planoId,
        Integer semana,
        LocalDate dataCheckin,
        Integer nivelEsforco,
        Integer nivelDor,
        Integer horasSonoSemana,
        String avaliacao,
        NivelRisco nivelRisco,
        boolean sobrecarga,
        boolean recuperacaoInsuficiente,
        boolean precisaReescreverPlano,
        String analiseIA,
        boolean planoReescrito
) {

    public static AnaliseRiscoDTO from(CheckinSemanal checkinSemanal) {
        if (checkinSemanal == null) {
            return null;
        }

        return new AnaliseRiscoDTO(
                checkinSemanal.getId(),
                checkinSemanal.getUsuario() == null ? null : checkinSemanal.getUsuario().getId(),
                checkinSemanal.getPlano() == null ? null : checkinSemanal.getPlano().getId(),
                checkinSemanal.getSemana(),
                checkinSemanal.getDataCheckin(),
                checkinSemanal.getNivelEsforco(),
                checkinSemanal.getNivelDor(),
                checkinSemanal.getHorasSonoSemana(),
                checkinSemanal.getAvaliacao(),
                checkinSemanal.avaliarNivelRisco(),
                checkinSemanal.indicaSobrecarga(),
                checkinSemanal.indicaRecuperacaoInsuficiente(),
                checkinSemanal.precisaReescreverPlano(),
                checkinSemanal.getAnaliseIA(),
                Boolean.TRUE.equals(checkinSemanal.getPlanoReescrito())
        );
    }
}
