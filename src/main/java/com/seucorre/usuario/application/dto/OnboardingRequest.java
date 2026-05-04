package com.seucorre.usuario.application.dto;

import jakarta.validation.Valid;

import java.util.List;

public record OnboardingRequest(
        String telefone,
        @Valid DadosFisicosRequest dadosFisicos,
        @Valid PerfilAtletaRequest perfilAtleta,
        @Valid PerfilCorridaRequest perfilCorrida,
        List<@Valid CondicaoSaudeRequest> condicoesSaude,
        List<@Valid DispositivoExternoRequest> dispositivos
) {}
