package com.seucorre.usuario.api;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.usuario.application.SyncAppService;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final SyncAppService service;

    @PostMapping("/vincular")
    public ResponseEntity<SyncAppService.DispositivoSincronizadoDTO> vincular(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestBody @Valid DispositivoExternoRequest request
    ) {
        SyncAppService.DispositivoSincronizadoDTO dispositivo = service.vincularDispositivo(usuarioLogado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dispositivo);
    }

    @PostMapping("/{plataforma}/sincronizar")
    public ResponseEntity<List<RegistroTreinoDTO>> sincronizar(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable PlataformaRelogio plataforma
    ) {
        return ResponseEntity.ok(service.sincronizarDados(usuarioLogado.getId(), plataforma));
    }

    @DeleteMapping("/{plataforma}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable PlataformaRelogio plataforma
    ) {
        service.removerDispositivo(usuarioLogado.getId(), plataforma);
        return ResponseEntity.noContent().build();
    }
}
