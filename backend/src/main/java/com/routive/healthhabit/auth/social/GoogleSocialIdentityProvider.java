package com.routive.healthhabit.auth.social;

import com.routive.healthhabit.common.error.*;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleSocialIdentityProvider implements SocialIdentityProvider {
    private final GoogleSocialProperties properties;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public GoogleSocialIdentityProvider(GoogleSocialProperties properties) {
        this(properties, defaultDecoder());
    }

    GoogleSocialIdentityProvider(GoogleSocialProperties properties, NimbusJwtDecoder decoder) {
        this.properties = properties;
        this.decoder = decoder;
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            boolean valid = List.of("accounts.google.com", "https://accounts.google.com").contains(issuer == null ? "" : issuer)
                    && jwt.getAudience().contains(properties.clientId())
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(Instant.now())
                    && jwt.getIssuedAt() != null && !jwt.getIssuedAt().isAfter(Instant.now().plusSeconds(60));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Google claims", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), claims));
    }

    private static NimbusJwtDecoder defaultDecoder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplate(factory)).build();
    }

    public String provider() { return "GOOGLE"; }

    public SocialIdentity verify(String token) {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED);
        }
        try {
            Jwt jwt = decoder.decode(token);
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank() || !Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }
            return new SocialIdentity(provider(), jwt.getSubject(), email, true);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
