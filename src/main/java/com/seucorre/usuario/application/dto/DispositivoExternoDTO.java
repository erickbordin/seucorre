package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.domain.DispositivoExterno;

import java.time.LocalDateTime;
import java.util.UUID;

public record DispositivoExternoDTO(
        UUID id,
        PlataformaRelogio plataforma,
        String descricaoPlataforma,
        boolean tokenValido,
        LocalDateTime tokenExpiresAt,
        boolean sincronizacaoDisponivel
) {

    public static DispositivoExternoDTO from(DispositivoExterno dispositivoExterno, boolean sincronizacaoDisponivel) {
        return new DispositivoExternoDTO(
                dispositivoExterno.getId(),
                dispositivoExterno.getPlataforma(),
                dispositivoExterno.getPlataforma() == null ? null : dispositivoExterno.getPlataforma().getDescricao(),
                dispositivoExterno.tokenValido(),
                dispositivoExterno.getTokenExpiresAt(),
                sincronizacaoDisponivel
        );
    }
}
