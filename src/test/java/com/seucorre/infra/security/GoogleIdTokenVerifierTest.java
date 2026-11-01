package com.seucorre.infra.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.seucorre.infra.security.GoogleIdTokenVerifier.IdentidadeGoogle;
import com.seucorre.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "seucorre-web.apps.googleusercontent.com";

    private static KeyPair chaves;
    private static GoogleIdTokenVerifier verifier;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        chaves = gerador.generateKeyPair();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) chaves.getPublic()).build();
        decoder.setJwtValidator(GoogleIdTokenVerifier.validador(CLIENT_ID));
        verifier = new GoogleIdTokenVerifier(decoder, true);
    }

    @Test
    void tokenValidoDoGoogleViraIdentidade() throws Exception {
        String token = assinar("https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300));

        assertThat(verifier.verificar(token))
                .contains(new IdentidadeGoogle("google-123", "ana@gmail.com", true, "Ana Souza"));
    }

    @Test
    void recusaTokenEmitidoParaOutroApp() throws Exception {
        String token = assinar("https://accounts.google.com", "outro-app.apps.googleusercontent.com", Instant.now().plusSeconds(300));

        assertThat(verifier.verificar(token)).isEmpty();
    }

    @Test
    void recusaTokenDeOutroEmissor() throws Exception {
        String token = assinar("https://emissor-falso.com", CLIENT_ID, Instant.now().plusSeconds(300));

        assertThat(verifier.verificar(token)).isEmpty();
    }

    @Test
    void recusaTokenExpirado() throws Exception {
        String token = assinar("accounts.google.com", CLIENT_ID, Instant.now().minusSeconds(600));

        assertThat(verifier.verificar(token)).isEmpty();
    }

    @Test
    void semClientIdConfiguradoAvisaQueOLoginNaoEstaDisponivel() {
        GoogleIdTokenVerifier semConfiguracao = new GoogleIdTokenVerifier("");

        assertThatThrownBy(() -> semConfiguracao.verificar("qualquer"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não está configurado");
    }

    private String assinar(String emissor, String audiencia, Instant expiracao) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(emissor)
                .audience(audiencia)
                .subject("google-123")
                .claim("email", "ana@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ana Souza")
                .issueTime(Date.from(expiracao.minusSeconds(3600)))
                .expirationTime(Date.from(expiracao))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(chaves.getPrivate()));
        return jwt.serialize();
    }
}
