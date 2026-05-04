package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.treino.domain.RegistroTreino;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistroTreinoDTO(
        UUID id,
        StatusTreino status,
        BigDecimal distanciaRealKm,
        Integer fcMedia,
        Integer fcMaxima,
        Integer paceMedioReal,
        Integer esforcoPercebido,
        Boolean sentiuDor,
        String localDor,
        Boolean doente,
        Boolean viagem,
        String observacao,
        Double desvioDistanciaPercent,
        Double desvioPace,
        boolean alertaSaude
) {

    public static RegistroTreinoDTO from(RegistroTreino registroTreino) {
        if (registroTreino == null) {
            return null;
        }

        BigDecimal distanciaPlanejada = registroTreino.getSessaoTreino() == null
                ? null
                : registroTreino.getSessaoTreino().getDistanciaKm();
        BigDecimal pacePlanejado = registroTreino.getSessaoTreino() == null
                ? null
                : registroTreino.getSessaoTreino().getPaceAlvo();

        return new RegistroTreinoDTO(
                registroTreino.getId(),
                registroTreino.getStatus(),
                registroTreino.getDistanciaRealKm(),
                registroTreino.getFcMedia(),
                registroTreino.getFcMaxima(),
                registroTreino.getPaceMedioReal(),
                registroTreino.getEsforcoPercebido(),
                registroTreino.getSentiuDor(),
                registroTreino.getLocalDor(),
                registroTreino.getDoente(),
                registroTreino.getViagem(),
                registroTreino.getObservacao(),
                registroTreino.calcularDesvioDistancia(distanciaPlanejada),
                registroTreino.calcularDesvioPace(pacePlanejado),
                registroTreino.temAlertaDeSaude()
        );
    }
}
