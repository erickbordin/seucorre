package com.seucorre.usuario.api;

import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.infra.security.JwtService;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository repository;
    private final JwtService tokenService;

    public AuthController(UsuarioRepository repository, JwtService tokenService) {
        this.repository = repository;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest data) {
        var usuario = this.repository.findByEmail(data.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Por enquanto, validamos a senha de forma simples (depois usaremos BCrypt)
        if (data.senha().equals(usuario.getSenha())) {
            var token = tokenService.gerarToken(usuario);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.badRequest().build();
    }

    // Record interno para facilitar a resposta
    public record LoginResponse(String token) { }
}