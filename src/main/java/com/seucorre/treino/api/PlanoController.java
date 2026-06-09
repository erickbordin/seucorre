package com.seucorre.treino.api;

import com.seucorre.treino.application.PlanoAppService;
import com.seucorre.treino.application.dto.GerarPlanoRequest;
import com.seucorre.treino.application.dto.PlanoTreinoDTO;
import com.seucorre.usuario.domain.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoAppService service;

    @PostMapping("/gerar")
    public ResponseEntity<PlanoTreinoDTO> gerarPlano(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestBody @Valid GerarPlanoRequest request
    ) {
        PlanoTreinoDTO planoGerado = service.criarPlano(usuarioLogado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(planoGerado);
    }

    @GetMapping("/me")
    public ResponseEntity<List<PlanoTreinoDTO>> listarMeusPlanos(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.listarPlanosDoUsuario(usuarioLogado.getId()));
    }

    @GetMapping("/{planoId}")
    public ResponseEntity<PlanoTreinoDTO> buscarPlano(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID planoId
    ) {
        return ResponseEntity.ok(service.buscarPlano(usuarioLogado.getId(), planoId));
    }

    @PatchMapping("/{planoId}/pausar")
    public ResponseEntity<PlanoTreinoDTO> pausarPlano(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID planoId
    ) {
        return ResponseEntity.ok(service.pausarPlano(usuarioLogado.getId(), planoId));
    }

    @PatchMapping("/{planoId}/reativar")
    public ResponseEntity<PlanoTreinoDTO> reativarPlano(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @PathVariable UUID planoId
    ) {
        return ResponseEntity.ok(service.reativarPlano(usuarioLogado.getId(), planoId));
    }
}
