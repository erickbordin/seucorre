package com.seucorre.usuario.application;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.shared.domain.enums.StatusTreino;
import com.seucorre.usuario.domain.DispositivoExterno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WearableAdapter {

    PlataformaRelogio plataformaSuportada();

    List<AtividadeExterna> sincronizarDados(DispositivoExterno dispositivoExterno);

    record AtividadeExterna(
            LocalDate dataRealizacao,
            StatusTreino status,
            BigDecimal distanciaKm,
            Integer duracaoRealMin,
            Integer fcMedia,
            Integer fcMaxima,
            Integer paceMedio,
            Integer esforcoPercebido,
            Boolean sentiuDor,
            Boolean doente,
            String observacao
    ) {
    }
}
