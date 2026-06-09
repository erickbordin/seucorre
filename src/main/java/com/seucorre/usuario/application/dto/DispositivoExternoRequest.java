package com.seucorre.usuario.application.dto;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DispositivoExternoRequest(
        @NotNull PlataformaRelogio plataforma,
        @NotBlank String tokenAcesso,
        @NotNull @Future LocalDateTime tokenExpiresAt
) {}
