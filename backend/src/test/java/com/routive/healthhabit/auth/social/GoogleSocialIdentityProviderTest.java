package com.routive.healthhabit.auth.social;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import com.routive.healthhabit.common.error.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import static org.assertj.core.api.Assertions.*;

class GoogleSocialIdentityProviderTest {
    private static KeyPair keys;
    private GoogleSocialIdentityProvider provider;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
    }

    @BeforeEach
    void setup() {
        provider = new GoogleSocialIdentityProvider(new GoogleSocialProperties("our-client"),
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build());
    }

    @Test
    void acceptsLocallySignedTokenWithExpectedClaims() throws Exception {
        SocialIdentity identity = provider.verify(token("valid"));
        assertThat(identity.providerUserId()).isEqualTo("google-user");
        assertThat(identity.email()).isEqualTo("verified@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"audience", "issuer", "expired", "missingExpiry", "missingSubject", "unverified", "signature", "futureIssued"})
    void rejectsInvalidToken(String mutation) throws Exception {
        String token = token(mutation);
        assertThatThrownBy(() -> provider.verify(token)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN));
    }

    @Test
    void missingClientIdIsConfigurationError() {
        var missing = new GoogleSocialIdentityProvider(new GoogleSocialProperties(""),
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build());
        assertThatThrownBy(() -> missing.verify("anything")).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED));
    }

    private String token(String mutation) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(mutation.equals("issuer") ? "https://attacker.invalid" : "https://accounts.google.com")
                .audience(mutation.equals("audience") ? "other-app" : "our-client")
                .issueTime(Date.from(now.plusSeconds(mutation.equals("futureIssued") ? 300 : -10)))
                .claim("email", "verified@example.com").claim("email_verified", !mutation.equals("unverified"));
        if (!mutation.equals("missingSubject")) claims.subject("google-user");
        if (!mutation.equals("missingExpiry")) claims.expirationTime(Date.from(now.plusSeconds(mutation.equals("expired") ? -120 : 300)));
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        PrivateKey key = keys.getPrivate();
        if (mutation.equals("signature")) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            key = generator.generateKeyPair().getPrivate();
        }
        jwt.sign(new RSASSASigner((RSAPrivateKey) key));
        return jwt.serialize();
    }
}
