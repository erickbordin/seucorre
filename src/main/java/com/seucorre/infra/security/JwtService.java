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
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private static final String ISSUER = "seucorre-api";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private long accessTokenExpirationHours;

    @Value("${api.security.token.refresh-expiration-days:7}")
    private long refreshTokenExpirationDays;

    public String gerarToken(Usuario usuario) {
        return gerarAccessToken(usuario);
    }

    public String gerarAccessToken(Usuario usuario) {
        return gerarToken(usuario, ACCESS_TOKEN_TYPE, Duration.ofHours(accessTokenExpirationHours));
    }

    public String gerarRefreshToken(Usuario usuario) {
        return gerarToken(usuario, REFRESH_TOKEN_TYPE, Duration.ofDays(refreshTokenExpirationDays));
    }

    public String validarToken(String token) {
        try {
            return extrairSubjectPorTipo(token, ACCESS_TOKEN_TYPE);
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    public String validarRefreshToken(String token) {
        try {
            return extrairSubjectPorTipo(token, REFRESH_TOKEN_TYPE);
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

    private String extrairSubjectPorTipo(String token, String tipoEsperado) {
        Claims claims = extrairClaims(token);
        String tipoToken = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!tipoEsperado.equals(tipoToken)) {
            throw new SecurityException("Tipo de token inválido.");
        }
        return claims.getSubject();
    }

    private String gerarToken(Usuario usuario, String tipoToken, Duration expiracao) {
        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("Usuário com e-mail válido é obrigatório para gerar token.");
        }
        if (tipoToken == null || tipoToken.isBlank()) {
            throw new IllegalArgumentException("Tipo de token é obrigatório.");
        }
        if (expiracao == null || expiracao.isZero() || expiracao.isNegative()) {
            throw new IllegalArgumentException("Expiração do token deve ser positiva.");
        }

        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expiracao);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(usuario.getEmail())
                .claim(TOKEN_TYPE_CLAIM, tipoToken)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chaveAssinatura())
                .compact();
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
