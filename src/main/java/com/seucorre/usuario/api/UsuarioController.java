package com.seucorre.usuario.api;

import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.UsuarioAppService;
import com.seucorre.usuario.application.dto.OnboardingRequest;
import com.seucorre.usuario.application.dto.UsuarioResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
