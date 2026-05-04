package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.TipoTreino;
import com.seucorre.treino.domain.SessaoTreino;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SessaoTreinoDTO(
        UUID id,
        Integer numeroSemana,
        LocalDate dataPrevista,
        TipoTreino tipo,
        BigDecimal distanciaKm,
        Integer duracaoMinutos,
        String intensidade,
        String zonaFcAlvo,
        BigDecimal paceAlvo,
        String descricao,
        boolean executada,
        boolean atrasada,
        RegistroTreinoDTO registro
) {

    public static SessaoTreinoDTO from(SessaoTreino sessaoTreino) {
        if (sessaoTreino == null) {
            return null;
        }
        return new SessaoTreinoDTO(
                sessaoTreino.getId(),
                sessaoTreino.getNumeroSemana(),
                sessaoTreino.getDataPrevista(),
                sessaoTreino.getTipo(),
                sessaoTreino.getDistanciaKm(),
                sessaoTreino.getDuracaoMinutos(),
                sessaoTreino.getIntensidade(),
                sessaoTreino.getZonaFcAlvo(),
                sessaoTreino.getPaceAlvo(),
                sessaoTreino.getDescricao(),
                sessaoTreino.foiExecutada(),
                sessaoTreino.estaAtrasada(),
                RegistroTreinoDTO.from(sessaoTreino.obterRegistro())
        );
    }
}
