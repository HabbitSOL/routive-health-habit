package com.routive.healthhabit.auth.social;

import com.routive.healthhabit.common.error.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KakaoSocialIdentityProviderTest {
    private MockRestServiceServer server;
    private KakaoSocialIdentityProvider provider;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://kapi.kakao.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new KakaoSocialIdentityProvider(new KakaoSocialProperties("12345"), builder.build());
    }

    @Test
    void validatesAppAndUserBeforeAcceptingEmail() {
        expectInfo("12345", 600);
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("{\"id\":42,\"kakao_account\":{\"email\":\"a@example.com\",\"is_email_verified\":true,\"is_email_valid\":true}}", MediaType.APPLICATION_JSON));
        assertThat(provider.verify("token").email()).isEqualTo("a@example.com");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongApp", "expired", "userMismatch", "unavailable", "unauthorized"})
    void rejectsInvalidOrUnavailableResponses(String scenario) {
        if (scenario.equals("unavailable")) {
            server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info")).andRespond(withServerError());
        } else if (scenario.equals("unauthorized")) {
            server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        } else {
            expectInfo(scenario.equals("wrongApp") ? "98765" : "12345", scenario.equals("expired") ? 0 : 600);
            if (scenario.equals("userMismatch")) server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                    .andRespond(withSuccess("{\"id\":43}", MediaType.APPLICATION_JSON));
        }
        assertThatThrownBy(() -> provider.verify("token")).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(scenario.equals("unavailable")
                        ? ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE : ErrorCode.INVALID_SOCIAL_TOKEN));
        server.verify();
    }

    @Test
    void unverifiedEmailIsNotTrusted() {
        expectInfo("12345", 600);
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("{\"id\":42,\"kakao_account\":{\"email\":\"a@example.com\",\"is_email_verified\":false}}", MediaType.APPLICATION_JSON));
        assertThat(provider.verify("token").email()).isNull();
        server.verify();
    }

    private void expectInfo(String appId, int expires) {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("{\"id\":42,\"app_id\":" + appId + ",\"expires_in\":" + expires + "}", MediaType.APPLICATION_JSON));
    }
}
