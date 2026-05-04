package com.seucorre.usuario.application.dto;

import java.util.List;

public record OnboardingRequest(
        String telefone,
        DadosFisicosRequest dadosFisicos,
        PerfilAtletaRequest perfilAtleta,
        PerfilCorridaRequest perfilCorrida,
        List<CondicaoSaudeRequest> condicoesSaude,
        List<DispositivoExternoRequest> dispositivos
) {}
