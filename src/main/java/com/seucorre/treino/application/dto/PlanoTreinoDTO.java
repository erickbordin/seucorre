package com.seucorre.treino.application.dto;

import com.seucorre.shared.domain.enums.StatusPlano;
import com.seucorre.treino.domain.PlanoTreino;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanoTreinoDTO(
        UUID id,
        Integer totalSemanas,
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal kmTotais,
        StatusPlano status,
        String resumoIA,
        String guiaTiposTreino,
        double progressoGeral,
        List<SessaoTreinoDTO> sessoes
) {

    public static PlanoTreinoDTO from(PlanoTreino planoTreino) {
        if (planoTreino == null) {
            return null;
        }
        return new PlanoTreinoDTO(
                planoTreino.getId(),
                planoTreino.getTotalSemanas(),
                planoTreino.getDataInicio(),
                planoTreino.getDataFim(),
                planoTreino.getKmTotais(),
                planoTreino.getStatus(),
                planoTreino.getResumoIA(),
                planoTreino.getGuiaTiposTreino(),
                planoTreino.calcularProgressoGeral(),
                planoTreino.getSessoes().stream().map(SessaoTreinoDTO::from).toList()
        );
    }
}
