package com.seucorre.infra.security;

import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "seucorre_super_secreto_123");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationHours", 2L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationDays", 7L);
    }

    @Test
    void accessTokenValidoAutenticaSomenteNoFluxoDeAcesso() {
        Usuario usuario = criarUsuario();
        String accessToken = jwtService.gerarAccessToken(usuario);

        assertThat(jwtService.validarToken(accessToken)).isEqualTo(usuario.getEmail());
        assertThat(jwtService.validarRefreshToken(accessToken)).isNull();
    }

    @Test
    void refreshTokenValidoAutenticaSomenteNoFluxoDeRefresh() {
        Usuario usuario = criarUsuario();
        String refreshToken = jwtService.gerarRefreshToken(usuario);

        assertThat(jwtService.validarRefreshToken(refreshToken)).isEqualTo(usuario.getEmail());
        assertThat(jwtService.validarToken(refreshToken)).isNull();
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("ana@email.com");
        return usuario;
    }
}
