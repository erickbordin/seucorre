package com.seucorre.usuario.api;

import com.seucorre.shared.domain.valueobjects.IMC;
import com.seucorre.shared.domain.enums.NivelCondicionamento;
import com.seucorre.shared.domain.enums.Objetivo;
import com.seucorre.usuario.application.UsuarioAppService;
import com.seucorre.usuario.application.dto.OnboardingRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private UsuarioAppService service;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        service = mock(UsuarioAppService.class);
        controller = new UsuarioController(service);
    }

    @Test
    void buscarMeuPerfilRetornaUsuarioLogado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());

        UsuarioResponse responseEsperado = usuarioResponse(usuarioLogado.getId());
        when(service.buscarPorId(usuarioLogado.getId())).thenReturn(responseEsperado);

        ResponseEntity<UsuarioResponse> response = controller.buscarMeuPerfil(usuarioLogado);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void atualizarMeuOnboardingUsaIdDoUsuarioLogado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        OnboardingRequest request = new OnboardingRequest(null, null, null, null, null, null);
        UsuarioResponse responseEsperado = usuarioResponse(usuarioLogado.getId());

        when(service.atualizarOnboarding(usuarioLogado.getId(), request)).thenReturn(responseEsperado);

        ResponseEntity<UsuarioResponse> response = controller.atualizarMeuOnboarding(usuarioLogado, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseEsperado);
        verify(service).atualizarOnboarding(usuarioLogado.getId(), request);
    }

    @Test
    void consultarMeuImcRetornaMetricaDoUsuarioLogado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        IMC imc = IMC.calcular(62.5d, 1.68d);

        when(service.calcularImc(usuarioLogado.getId())).thenReturn(imc);

        ResponseEntity<IMC> response = controller.consultarMeuImc(usuarioLogado);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(imc);
    }

    private UsuarioResponse usuarioResponse(UUID usuarioId) {
        return new UsuarioResponse(
                usuarioId,
                "Ana Runner",
                "ana@email.com",
                new BigDecimal("62.50"),
                new BigDecimal("168.00"),
                LocalDate.of(1995, 5, 10),
                "FEMININO",
                NivelCondicionamento.INTERMEDIARIO,
                Objetivo.COMPLETAR_10K,
                true,
                false,
                7,
                4,
                "SEG,QUA,SEX,SAB",
                58,
                190,
                true,
                null,
                List.of(),
                List.of(),
                LocalDate.of(2026, 5, 5)
        );
    }
}
