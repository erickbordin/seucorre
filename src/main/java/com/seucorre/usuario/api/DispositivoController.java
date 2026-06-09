package com.seucorre.usuario.api;

import com.seucorre.shared.domain.enums.PlataformaRelogio;
import com.seucorre.usuario.application.SyncAppService;
import com.seucorre.usuario.application.dto.DispositivoExternoDTO;
import com.seucorre.usuario.application.dto.DispositivoExternoRequest;
import com.seucorre.usuario.application.dto.SincronizacaoDispositivoDTO;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ResponseEntity<List<DispositivoExternoDTO>> listar(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.listarDispositivos(usuarioLogado.getId()));
    }

    @PostMapping
    public ResponseEntity<DispositivoExternoDTO> vincular(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestBody @Valid DispositivoExternoRequest request
    ) {
        return ResponseEntity.ok(service.vincularDispositivo(usuarioLogado.getId(), request));
    }

    @PostMapping("/{plataforma}/sincronizar")
    public ResponseEntity<SincronizacaoDispositivoDTO> sincronizar(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable PlataformaRelogio plataforma
    ) {
        return ResponseEntity.ok(service.sincronizarDispositivo(usuarioLogado.getId(), plataforma));
    }
}
