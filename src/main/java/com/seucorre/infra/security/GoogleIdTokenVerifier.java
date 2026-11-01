package com.seucorre.infra.security;

import com.seucorre.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Component
public class GoogleIdTokenVerifier {

    private static final String CHAVES_PUBLICAS_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISSORES_GOOGLE = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;
    private final boolean configurado;

    @Autowired
    public GoogleIdTokenVerifier(@Value("${seucorre.auth.google.client-id:}") String clientId) {
        this(criarDecoder(clientId), clientId != null && !clientId.isBlank());
    }

    GoogleIdTokenVerifier(JwtDecoder decoder, boolean configurado) {
        this.decoder = decoder;
        this.configurado = configurado;
    }

    static OAuth2TokenValidator<Jwt> validador(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, emissor -> EMISSORES_GOOGLE.contains(String.valueOf(emissor))),
                new JwtClaimValidator<Object>(JwtClaimNames.AUD, audiencia -> audiencia instanceof Collection<?> lista && lista.contains(clientId))
        );
    }

    public Optional<IdentidadeGoogle> verificar(String idToken) {
        if (!configurado) {
            throw new BusinessRuleException("Login com Google não está configurado.");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            return Optional.of(new IdentidadeGoogle(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    jwt.getClaimAsString("name")
            ));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private static JwtDecoder criarDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CHAVES_PUBLICAS_GOOGLE).build();
        decoder.setJwtValidator(validador(clientId));
        return decoder;
    }

    public record IdentidadeGoogle(String googleId, String email, boolean emailVerificado, String nome) {
    }
}
