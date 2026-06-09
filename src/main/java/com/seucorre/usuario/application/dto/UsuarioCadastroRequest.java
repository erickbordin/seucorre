package com.seucorre.usuario.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioCadastroRequest(
    @NotBlank String nome,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String senha,
    String telefone,
    @Valid DadosFisicosRequest dadosFisicos,
    @Valid PerfilAtletaRequest perfilAtleta,
    @Valid PerfilCorridaRequest perfilCorrida,
    List<@Valid CondicaoSaudeRequest> condicoesSaude,
    List<@Valid DispositivoExternoRequest> dispositivos
) {}
