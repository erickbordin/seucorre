package com.seucorre.usuario.api;

import com.seucorre.infra.security.JwtService;
import com.seucorre.shared.api.ApiResponse;
import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.usuario.application.dto.LoginResponse;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
        when(jwtService.gerarAccessToken(usuario)).thenReturn("jwt-token");
        when(jwtService.gerarRefreshToken(usuario)).thenReturn("refresh-token");

        ResponseEntity<?> response = controller.login(new LoginRequest("ana@email.com", "senha123"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new LoginResponse("jwt-token", "refresh-token"));
    }

    @Test
    void loginRetornaUnauthorizedComMensagemQuandoSenhaNaoConfere() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@email.com");
        usuario.setSenhaHash("hash-bcrypt");

        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

        ResponseEntity<?> response = controller.login(new LoginRequest("ana@email.com", "errada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("Senha incorreta."));
    }

    @Test
    void loginRetornaUnauthorizedComMensagemQuandoUsuarioNaoExiste() {
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(new LoginRequest("ana@email.com", "senha123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(ApiResponse.erro("E-mail incorreto."));
    }

    @Test
    void refreshRetornaNovoTokenQuandoBearerValido() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@email.com");

        when(jwtService.validarRefreshToken("refresh-antigo")).thenReturn("ana@email.com");
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarAccessToken(usuario)).thenReturn("token-novo");
        when(jwtService.gerarRefreshToken(usuario)).thenReturn("refresh-novo");

        ResponseEntity<?> response = controller.refresh("Bearer refresh-antigo");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new LoginResponse("token-novo", "refresh-novo"));
    }

    @Test
    void refreshRetornaUnauthorizedQuandoHeaderNaoPossuiBearer() {
        ResponseEntity<?> response = controller.refresh("token-sem-prefixo");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshRetornaUnauthorizedQuandoTokenInvalido() {
        when(jwtService.validarRefreshToken("token-invalido")).thenReturn(null);

        ResponseEntity<?> response = controller.refresh("Bearer token-invalido");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
