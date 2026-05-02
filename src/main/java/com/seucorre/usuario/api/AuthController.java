package com.seucorre.usuario.api;

import com.seucorre.usuario.application.dto.LoginRequest;
import com.seucorre.infra.security.JwtService;
import com.seucorre.usuario.infrastructure.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository repository;
    private final JwtService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository repository, JwtService tokenService, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest data) {
        var usuario = this.repository.findByEmail(data.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (passwordEncoder.matches(data.senha(), usuario.getSenhaHash())) {
            var token = tokenService.gerarToken(usuario);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.badRequest().build();
    }

    // Record interno para facilitar a resposta
    public record LoginResponse(String token) { }
}
