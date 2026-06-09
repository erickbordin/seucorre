package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CondicaoSaudeRequest(
        @NotBlank String tipo,
        String descricao,
        Boolean ativa
) {}
