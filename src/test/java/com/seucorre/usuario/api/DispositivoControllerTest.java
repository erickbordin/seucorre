package com.seucorre.usuario.api;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.application.SyncAppService;
import com.seucorre.usuario.application.dto.DispositivoExternoDTO;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.SincronizacaoDispositivoDTO;
import com.seucorre.usuario.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispositivoControllerTest {

    private SyncAppService service;
    private DispositivoController controller;

    @BeforeEach
    void setUp() {
        service = mock(SyncAppService.class);
        controller = new DispositivoController(service);
    }

    @Test
    void listarUsaUsuarioAutenticado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        List<DispositivoExternoDTO> dispositivos = List.of(new DispositivoExternoDTO(
                UUID.randomUUID(),
                PlataformaRelogio.GARMIN,
                "Garmin Connect",
                true,
                LocalDateTime.now().plusDays(3),
                true
        ));
        when(service.listarDispositivos(usuarioLogado.getId())).thenReturn(dispositivos);

        ResponseEntity<List<DispositivoExternoDTO>> response = controller.listar(usuarioLogado);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dispositivos);
        verify(service).listarDispositivos(usuarioLogado.getId());
    }

    @Test
    void vincularUsaUsuarioAutenticado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        DispositivoExternoRequest request = new DispositivoExternoRequest(
                PlataformaRelogio.GARMIN,
                "token-garmin",
                LocalDateTime.now().plusDays(7)
        );
        DispositivoExternoDTO responseEsperado = new DispositivoExternoDTO(
                UUID.randomUUID(),
                PlataformaRelogio.GARMIN,
                "Garmin Connect",
                true,
                request.tokenExpiresAt(),
                true
        );
        when(service.vincularDispositivo(usuarioLogado.getId(), request)).thenReturn(responseEsperado);

        ResponseEntity<DispositivoExternoDTO> response = controller.vincular(usuarioLogado, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseEsperado);
        verify(service).vincularDispositivo(usuarioLogado.getId(), request);
    }

    @Test
    void sincronizarUsaUsuarioAutenticado() {
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        SincronizacaoDispositivoDTO responseEsperado = new SincronizacaoDispositivoDTO(
                PlataformaRelogio.GARMIN,
                0,
                List.of()
        );
        when(service.sincronizarDispositivo(usuarioLogado.getId(), PlataformaRelogio.GARMIN)).thenReturn(responseEsperado);

        ResponseEntity<SincronizacaoDispositivoDTO> response = controller.sincronizar(usuarioLogado, PlataformaRelogio.GARMIN);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseEsperado);
        verify(service).sincronizarDispositivo(usuarioLogado.getId(), PlataformaRelogio.GARMIN);
    }
}
