package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.PlataformaRelogio;

import java.time.LocalDateTime;

public record DispositivoExternoRequest(
        PlataformaRelogio plataforma,
        String tokenAcesso,
        LocalDateTime tokenExpiresAt
) {}
