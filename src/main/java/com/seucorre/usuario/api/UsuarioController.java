package com.seucorre.usuario.api;

import com.seucorre.usuario.application.dto.UsuarioCadastroRequest;
import com.seucorre.usuario.application.UsuarioAppService;
import com.seucorre.usuario.domain.Usuario;
import com.seucorre.usuario.infrastructure.UsuarioRepository; // <-- ADICIONADO: Importação do repositório

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios") 
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioAppService service;
    private final UsuarioRepository repository; // <-- ADICIONADO: Injeção do repositório

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody @Valid UsuarioCadastroRequest request) {
        Usuario usuarioSalvo = service.registrar(request); 
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSalvo);
    }
   
    @GetMapping 
    public ResponseEntity<List<Usuario>> listarTodos() {
        List<Usuario> usuarios = repository.findAll();
        return ResponseEntity.ok(usuarios);
    }
}