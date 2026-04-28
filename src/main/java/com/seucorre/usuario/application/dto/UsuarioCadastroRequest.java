package com.seucorre.usuario.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioCadastroRequest(
    @NotBlank String nome,
    @NotBlank @Email String email,
    @NotBlank String senha
) {}