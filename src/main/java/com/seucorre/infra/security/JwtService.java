package com.seucorre.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import com.seucorre.usuario.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private static final String ISSUER = "seucorre-api";

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private long expirationHours;

    public String gerarToken(Usuario usuario) {
        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("Usuário com e-mail válido é obrigatório para gerar token.");
        }

        Instant agora = Instant.now();
        Instant expiracao = agora.plus(Duration.ofHours(expirationHours));

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.getEmail())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(chaveAssinatura())
                .compact();
    }

    public String validarToken(String token) {
        try {
            return extrairSubject(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    public String extrairSubject(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public <T> T extrairClaim(String token, Function<Claims, T> claimsResolver) {
        if (claimsResolver == null) {
            throw new IllegalArgumentException("Claims resolver é obrigatório.");
        }
        return claimsResolver.apply(extrairClaims(token));
    }

    public Claims extrairClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token é obrigatório.");
        }

        Claims claims = Jwts.parser()
                .verifyWith(chaveAssinatura())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!ISSUER.equals(claims.getIssuer())) {
            throw new SecurityException("Issuer do token inválido.");
        }

        return claims;
    }

    private SecretKey chaveAssinatura() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Segredo JWT não configurado.");
        }

        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível para derivar a chave JWT.", exception);
        }
    }
}
