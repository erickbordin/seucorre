package com.seucorre.usuario.api;

import com.seucorre.infra.security.JwtService;
import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UsuarioRepository repository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new AuthController(repository, jwtService, passwordEncoder);
    }

    @Test
    void loginRetornaTokenQuandoSenhaBateComHash() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("ana@email.com");
        usuario.setSenhaHash("hash-bcrypt");

        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hash-bcrypt")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.login(new LoginRequest("ana@email.com", "senha123"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new AuthController.LoginResponse("jwt-token"));
    }

    @Test
    void loginRetornaBadRequestQuandoSenhaNaoConfere() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@email.com");
        usuario.setSenhaHash("hash-bcrypt");

        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

        ResponseEntity<?> response = controller.login(new LoginRequest("ana@email.com", "errada"));

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
