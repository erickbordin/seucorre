package com.seucorre.usuario.api;

import com.seucorre.shared.domain.valueobjects.IMC;
import com.seucorre.usuario.application.UsuarioAppService;
import com.seucorre.usuario.application.dto.OnboardingRequest;
import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;
import com.seucorre.usuario.domain.Usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios") 
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioAppService service;

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid UsuarioCadastroRequest request) {
        UsuarioResponse usuarioSalvo = service.registrar(request); 
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> buscarMeuPerfil(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.buscarPorId(usuarioLogado.getId()));
    }

    @PutMapping("/me/onboarding")
    public ResponseEntity<UsuarioResponse> atualizarMeuOnboarding(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestBody @Valid OnboardingRequest request
    ) {
        return ResponseEntity.ok(service.atualizarOnboarding(usuarioLogado.getId(), request));
    }

    @GetMapping("/me/imc")
    public ResponseEntity<IMC> consultarMeuImc(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(service.calcularImc(usuarioLogado.getId()));
    }

    @PutMapping("/{id}/onboarding")
    public ResponseEntity<UsuarioResponse> atualizarOnboarding(
            @PathVariable UUID id,
            @RequestBody @Valid OnboardingRequest request
    ) {
        return ResponseEntity.ok(service.atualizarOnboarding(id, request));
    }

    @GetMapping 
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }
}
