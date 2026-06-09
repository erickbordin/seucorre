package com.seucorre.infra.security;

import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private UsuarioRepository repository;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        repository = mock(UsuarioRepository.class);
        filter = new JwtAuthFilter(jwtService, repository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaUsuarioQuandoBearerTokenValido() throws ServletException, IOException {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("ana@email.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        when(jwtService.validarToken("token-valido")).thenReturn("ana@email.com");
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(usuario);
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_USER");
    }

    @Test
    void naoAutenticaQuandoHeaderAuthorizationNaoPossuiBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "token-sem-prefixo");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, repository);
    }

    @Test
    void preservaAutenticacaoExistenteNoContexto() throws ServletException, IOException {
        Usuario usuarioAutenticado = new Usuario();
        usuarioAutenticado.setId(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioAutenticado, null)
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer outro-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(usuarioAutenticado);
        verifyNoInteractions(jwtService, repository);
    }

    @Test
    void consultaRepositorioQuandoJwtValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        when(jwtService.validarToken("token-valido")).thenReturn("ana@email.com");
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.empty());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService).validarToken("token-valido");
        verify(repository).findByEmail("ana@email.com");
    }
}
