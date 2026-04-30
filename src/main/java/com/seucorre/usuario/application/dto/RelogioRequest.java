package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record RelogioRequest(
        @NotBlank String plataforma,
        String tokenAcesso,
        LocalDateTime tokenExpiresAt
) {}
