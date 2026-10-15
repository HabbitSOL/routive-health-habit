package com.routive.healthhabit.auth.social;

import com.routive.healthhabit.common.error.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class KakaoSocialIdentityProvider implements SocialIdentityProvider {
    private final RestClient client;
    private final KakaoSocialProperties properties;

    @Autowired
    public KakaoSocialIdentityProvider(KakaoSocialProperties properties) {
        this(properties, defaultClient());
    }

    KakaoSocialIdentityProvider(KakaoSocialProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    private static RestClient defaultClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl("https://kapi.kakao.com").requestFactory(factory).build();
    }

    public String provider() { return "KAKAO"; }

    @SuppressWarnings("unchecked")
    public SocialIdentity verify(String token) {
        if (properties.appId() == null || properties.appId().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED);
        }
        try {
            Map<String, Object> info = get("/v1/user/access_token_info", token);
            if (info == null || info.get("id") == null
                    || !properties.appId().equals(String.valueOf(info.get("app_id")))
                    || !(info.get("expires_in") instanceof Number lifetime) || lifetime.longValue() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }
            Map<String, Object> body = get("/v2/user/me", token);
            if (body == null || body.get("id") == null
                    || !String.valueOf(info.get("id")).equals(String.valueOf(body.get("id")))) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }
            Map<String, Object> account = (Map<String, Object>) body.get("kakao_account");
            String email = account == null ? null : (String) account.get("email");
            boolean verified = account != null && Boolean.TRUE.equals(account.get("is_email_verified"))
                    && Boolean.TRUE.equals(account.get("is_email_valid"));
            return new SocialIdentity(provider(), String.valueOf(body.get("id")), verified ? email : null, verified);
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 429) throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE);
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        } catch (ResourceAccessException | HttpServerErrorException exception) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String token) {
        return client.get().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().body(Map.class);
    }
}
