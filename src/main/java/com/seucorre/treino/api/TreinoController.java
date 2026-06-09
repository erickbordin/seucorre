package com.seucorre.treino.api;

import com.seucorre.treino.application.TreinoAppService;
import com.seucorre.treino.application.dto.RegistroTreinoDTO;
import com.seucorre.treino.application.dto.RegistroTreinoRequest;
import com.seucorre.treino.application.dto.SessaoTreinoDTO;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/treinos")
@RequiredArgsConstructor
public class TreinoController {

    private final TreinoAppService service;

    @PatchMapping("/{treinoId}/registrar")
    public ResponseEntity<RegistroTreinoDTO> registrarExecucao(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID treinoId,
            @RequestBody @Valid RegistroTreinoRequest request
    ) {
        return ResponseEntity.ok(service.registrarExecucao(usuarioLogado.getId(), treinoId, request));
    }

    @GetMapping("/semana/{semana}")
    public ResponseEntity<List<SessaoTreinoDTO>> listarSessoesDaSemana(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable @Min(1) Integer semana
    ) {
        return ResponseEntity.ok(service.listarSessoesDaSemana(usuarioLogado.getId(), semana));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<SessaoTreinoDTO>> listarHistorico(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.listarHistoricoSessoes(usuarioLogado.getId()));
    }
}
