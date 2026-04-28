package com.seucorre.usuario.api;

import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.UsuarioAppService;
import com.seucorre.usuario.domain.Usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios") 
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioAppService service;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody @Valid UsuarioCadastroRequest request) {
        
        Usuario usuarioSalvo = service.registrar(request); 
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }
}