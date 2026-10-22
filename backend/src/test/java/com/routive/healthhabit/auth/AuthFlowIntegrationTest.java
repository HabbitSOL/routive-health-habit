package com.routive.healthhabit.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.routive.healthhabit.auth.domain.RefreshTokenRepository;
import com.routive.healthhabit.auth.domain.EmailVerificationTokenRepository;
import com.routive.healthhabit.auth.domain.PasswordResetTokenRepository;
import com.routive.healthhabit.auth.domain.SocialAccountRepository;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.user.domain.UserConsentRepository;
import com.routive.healthhabit.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserConsentRepository userConsentRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired private com.routive.healthhabit.user.domain.UserProfileRepository profileRepository;
    @Autowired private com.routive.healthhabit.auth.application.TokenService tokenService;

    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private SocialAccountRepository socialAccountRepository;
    @Autowired private com.routive.healthhabit.auth.domain.AuthSessionRepository authSessionRepository;

    @MockitoBean
    private AccountMailSender accountMailSender;

    @Test
    void loginRateLimitAlsoProtectsNonexistentAccounts() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .with(request -> { request.setRemoteAddr("127.0.0.99"); return request; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"rate-limit-fixture@example.invalid\",\"password\":\"fake-password-123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> { request.setRemoteAddr("127.0.0.99"); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rate-limit-fixture@example.invalid\",\"password\":\"fake-password-123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @BeforeEach
    void cleanDatabase() {
        reset(accountMailSender);
        refreshTokenRepository.deleteAll();
        authSessionRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        socialAccountRepository.deleteAll();
        userConsentRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signupThenAccessMyProfileWithJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING_EMAIL_VERIFICATION"));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendVerification(org.mockito.ArgumentMatchers.eq("User@Example.com"), tokenCaptor.capture());

        MvcResult verification = mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + tokenCaptor.getValue() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        String accessToken = JsonPath.read(verification.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("User@Example.com"))
                .andExpect(jsonPath("$.data.consents.length()").value(3));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void passwordResetChangesPasswordAndRevokesSessions() throws Exception {
        Session session = signupAndVerify();
        reset(accountMailSender);

        mockMvc.perform(post("/api/v1/auth/password-resets/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendPasswordReset(org.mockito.ArgumentMatchers.eq("User@Example.com"), tokenCaptor.capture());

        mockMvc.perform(post("/api/v1/auth/password-resets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + tokenCaptor.getValue() + "\",\"newPassword\":\"new-password-5678\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + session.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"new-password-5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session session = signupAndVerify();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + session.refreshToken() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + session.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawalDisablesLogin() throws Exception {
        Session session = signupAndVerify();
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"strong-password-1234\"}"))
                .andExpect(status().isUnauthorized());
    }


    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @Autowired private com.routive.healthhabit.auth.application.AuthService authService;

    @Test
    void loginBeforeVerificationIsRejected() throws Exception {
        pendingSignup();
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"strong-password-1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verificationIsSingleUseAndExpiredTokensFail() throws Exception {
        String token = pendingSignup();
        setDatabaseTime("update email_verification_tokens set expires_at = ?", java.time.Instant.now().minusSeconds(10));
        confirmEmail(token).andExpect(status().isBadRequest());
        setDatabaseTime("update email_verification_tokens set expires_at = ?", java.time.Instant.now().plusSeconds(1800));
        confirmEmail(token).andExpect(status().isOk());
        confirmEmail(token).andExpect(status().isBadRequest());
    }

    @Test
    void resendHasCooldownAndInvalidatesPreviousLink() throws Exception {
        String old = pendingSignup();
        mockMvc.perform(post("/api/v1/auth/email-verifications/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk());
        verify(accountMailSender, org.mockito.Mockito.times(1)).sendVerification(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        setDatabaseTime("update email_verification_tokens set created_at = ?", java.time.Instant.now().minusSeconds(120));
        reset(accountMailSender);
        mockMvc.perform(post("/api/v1/auth/email-verifications/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk());
        ArgumentCaptor<String> captured = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendVerification(org.mockito.ArgumentMatchers.anyString(), captured.capture());
        confirmEmail(old).andExpect(status().isBadRequest());
        confirmEmail(captured.getValue()).andExpect(status().isOk());
    }

    @Test
    void refreshRotatesAndPreviousRefreshCannotBeUsedAgain() throws Exception {
        Session initial = signupAndVerify();
        MvcResult rotated = refresh(initial.refreshToken()).andExpect(status().isOk()).andReturn();
        String replacement = JsonPath.read(rotated.getResponse().getContentAsString(), "$.data.refreshToken");
        org.assertj.core.api.Assertions.assertThat(replacement).isNotEqualTo(initial.refreshToken());
        refresh(initial.refreshToken()).andExpect(status().isUnauthorized());
        refresh(replacement).andExpect(status().isOk());
        me(initial.accessToken()).andExpect(status().isOk());
    }

    @Test
    void concurrentRefreshOnlySucceedsOnce() throws Exception {
        Session session = signupAndVerify();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var ready = new java.util.concurrent.CountDownLatch(2);
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> call = () -> {
                ready.countDown();
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try { authService.refresh(session.refreshToken()); return true; }
                catch (com.routive.healthhabit.common.error.BusinessException exception) {
                    org.assertj.core.api.Assertions.assertThat(exception.errorCode())
                            .isEqualTo(com.routive.healthhabit.common.error.ErrorCode.INVALID_REFRESH_TOKEN);
                    return false;
                }
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            org.assertj.core.api.Assertions.assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(
                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void logoutBlocksAccessImmediatelyButKeepsOtherDevice() throws Exception {
        Session first = signupAndVerify();
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"strong-password-1234\"}"))
                .andExpect(status().isOk()).andReturn();
        String second = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + first.refreshToken() + "\"}"))
                .andExpect(status().isOk());
        me(first.accessToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        me(second).andExpect(status().isOk());
    }

    @Test
    void cannotLogoutAnotherUsersSession() throws Exception {
        Session first = signupAndVerify();
        reset(accountMailSender);
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson().replace("User@Example.com", "other@example.com"))).andExpect(status().isOk());
        ArgumentCaptor<String> captured = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendVerification(org.mockito.ArgumentMatchers.eq("other@example.com"), captured.capture());
        MvcResult verified = confirmEmail(captured.getValue()).andExpect(status().isOk()).andReturn();
        String otherRefresh = JsonPath.read(verified.getResponse().getContentAsString(), "$.data.refreshToken");
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + otherRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        refresh(otherRefresh).andExpect(status().isOk());
        me(first.accessToken()).andExpect(status().isOk());
    }

    @Test
    void resetIsSingleUseAndBlocksAllExistingAccessTokens() throws Exception {
        Session session = signupAndVerify();
        String token = requestReset();
        resetPassword(token).andExpect(status().isOk());
        resetPassword(token).andExpect(status().isBadRequest());
        me(session.accessToken()).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"strong-password-1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredResetAndUnknownEmailAreHandled() throws Exception {
        signupAndVerify();
        String token = requestReset();
        setDatabaseTime("update password_reset_tokens set expires_at = ?", java.time.Instant.now().minusSeconds(10));
        resetPassword(token).andExpect(status().isBadRequest());
        reset(accountMailSender);
        mockMvc.perform(post("/api/v1/auth/password-resets/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));
        org.mockito.Mockito.verifyNoInteractions(accountMailSender);
    }

    @Test
    void resetResendInvalidatesPreviousLinkAndEnforcesCooldown() throws Exception {
        signupAndVerify();
        String old = requestReset();
        mockMvc.perform(post("/api/v1/auth/password-resets/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk());
        verify(accountMailSender, org.mockito.Mockito.times(1)).sendPasswordReset(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        setDatabaseTime("update password_reset_tokens set created_at = ?", java.time.Instant.now().minusSeconds(120));
        String fresh = requestReset();
        resetPassword(old).andExpect(status().isBadRequest());
        resetPassword(fresh).andExpect(status().isOk());
    }

    @Test
    void withdrawalRevokesAccessAndPendingRecoveryAndAnonymizesData() throws Exception {
        Session session = signupAndVerify();
        String resetToken = requestReset();
        var user = userRepository.findByEmailNormalized("user@example.com").orElseThrow();
        socialAccountRepository.save(com.routive.healthhabit.auth.domain.SocialAccount.create(
                user, "KAKAO", "123", "user@example.com", java.time.Instant.now()));
        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"DELETE\"}")).andExpect(status().isOk());
        me(session.accessToken()).andExpect(status().isUnauthorized());
        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
        resetPassword(resetToken).andExpect(status().isBadRequest());
        var deleted = userRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.getStatus()).isEqualTo("DELETED");
        org.assertj.core.api.Assertions.assertThat(deleted.getEmail()).endsWith("@invalid.local");
        org.assertj.core.api.Assertions.assertThat(deleted.getPasswordHash()).isNull();
        org.assertj.core.api.Assertions.assertThat(socialAccountRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(userConsentRepository.findAllByUserIdOrderByConsentedAtDesc(user.getId()))
                .allMatch(consent -> consent.getWithdrawnAt() != null);
    }

    @Test
    void missingOrInvalidJwtUsesCommonErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        me("invalid.token").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void malformedJsonAndOversizedUnicodePasswordsAreBadRequests() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(signupJson().replace("strong-password-1234", "가".repeat(25))))
                .andExpect(status().isBadRequest());
    }

    private void setDatabaseTime(String sql, java.time.Instant instant) {
        String query = switch (sql) {
            case "update email_verification_tokens set expires_at = ?" -> "update EmailVerificationToken set expiresAt = :time";
            case "update email_verification_tokens set created_at = ?" -> "update EmailVerificationToken set createdAt = :time";
            case "update password_reset_tokens set expires_at = ?" -> "update PasswordResetToken set expiresAt = :time";
            case "update password_reset_tokens set created_at = ?" -> "update PasswordResetToken set createdAt = :time";
            case "update auth_sessions set expires_at = ?" -> "update AuthSession set expiresAt = :time";
            default -> throw new IllegalArgumentException("Unsupported test fixture update");
        };
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status ->
                entityManager.createQuery(query).setParameter("time", instant).executeUpdate());
    }

    private String pendingSignup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson()))
                .andExpect(status().isOk());
        ArgumentCaptor<String> captured = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendVerification(org.mockito.ArgumentMatchers.anyString(), captured.capture());
        return captured.getValue();
    }

    private String requestReset() throws Exception {
        reset(accountMailSender);
        mockMvc.perform(post("/api/v1/auth/password-resets/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk());
        ArgumentCaptor<String> captured = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendPasswordReset(org.mockito.ArgumentMatchers.anyString(), captured.capture());
        return captured.getValue();
    }

    private org.springframework.test.web.servlet.ResultActions confirmEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email-verifications/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions resetPassword(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-resets/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"new-password-5678\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token));
    }


    @Test
    void accountLinkPagesArePublicAndDoNotConsumeTokensOnGet() throws Exception {
        String token = pendingSignup();
        mockMvc.perform(get("/verify-email").param("token", token)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/auth-pages/verify-email.html"));
        mockMvc.perform(get("/reset-password")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/auth-pages/reset-password.html"));
        mockMvc.perform(get("/auth-pages/account.js")).andExpect(status().isOk());
        confirmEmail(token).andExpect(status().isOk());
    }

    @Test
    void expiredSessionRejectsAccessAndRefresh() throws Exception {
        Session session = signupAndVerify();
        setDatabaseTime("update auth_sessions set expires_at = ?", java.time.Instant.now().minusSeconds(10));
        me(session.accessToken()).andExpect(status().isUnauthorized());
        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentEmailConfirmationOnlySucceedsOnce() throws Exception {
        String token = pendingSignup();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> call = () -> {
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                return confirmEmail(token).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(
                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 400);
        }
    }

    @Test
    void concurrentPasswordResetOnlySucceedsOnce() throws Exception {
        signupAndVerify();
        String token = requestReset();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> call = () -> {
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                return resetPassword(token).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(
                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 400);
        }
    }

    @Test
    void profilePatchPreservesOmittedFieldsAndCanClearOptionalFields() throws Exception {
        Session s = signupAndVerify();
        patchProfile(s, """
                {"nickname":" 성준 ","birthDate":"2000-01-01","sex":"MALE","heightCm":175.25}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("성준"));
        patchProfile(s, "{\"nickname\":\"새이름\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heightCm").value(175.25));
        patchProfile(s, "{\"clearFields\":[\"birthDate\",\"sex\",\"heightCm\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.birthDate").isEmpty())
                .andExpect(jsonPath("$.data.heightCm").isEmpty());
        me(s.accessToken()).andExpect(jsonPath("$.data.profile.nickname").value("새이름"));
    }

    @Test
    void invalidProfileRequestsAreRejectedWithoutPartialSave() throws Exception {
        Session s = signupAndVerify();
        for (String body : java.util.List.of(
                "{\"nickname\":\"   \"}", "{\"birthDate\":\"2999-01-01\"}",
                "{\"sex\":\"BAD\"}", "{\"heightCm\":-1}", "{\"heightCm\":300.01}",
                "{\"heightCm\":170.123}", "{\"clearFields\":[\"nickname\"]}",
                "{\"heightCm\":170,\"clearFields\":[\"heightCm\"]}")) {
            patchProfile(s, body).andExpect(status().isBadRequest());
        }
        org.assertj.core.api.Assertions.assertThat(profileRepository.count()).isZero();
    }

    @Test
    void onboardingRequiresNicknameAndIsIdempotent() throws Exception {
        Session s = signupAndVerify();
        completeOnboarding(s).andExpect(status().isBadRequest());
        patchProfile(s, "{\"nickname\":\"성준\"}").andExpect(status().isOk());
        String first = completeOnboarding(s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingCompletedAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String timestamp = JsonPath.read(first, "$.data.onboardingCompletedAt");
        completeOnboarding(s).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingCompletedAt").value(timestamp));
    }

    @Test
    void consentWithdrawalBlocksWritesAndReacceptancePreservesHistory() throws Exception {
        Session s = signupAndVerify();
        changeConsent(s, "SENSITIVE_HEALTH_DATA", "2026-10", false).andExpect(status().isOk());
        changeConsent(s, "SENSITIVE_HEALTH_DATA", "2026-10", false).andExpect(status().isOk());
        patchProfile(s, "{\"nickname\":\"성준\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUIRED_CONSENT_MISSING"));
        completeOnboarding(s).andExpect(status().isBadRequest());
        me(s.accessToken()).andExpect(status().isOk());
        changeConsent(s, "SENSITIVE_HEALTH_DATA", "2026-10", true).andExpect(status().isOk());
        changeConsent(s, "SENSITIVE_HEALTH_DATA", "2026-10", true).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(userConsentRepository.count()).isEqualTo(4);
        var health = userConsentRepository.findAll().stream()
                .filter(c -> c.getConsentType().equals("SENSITIVE_HEALTH_DATA")).toList();
        org.assertj.core.api.Assertions.assertThat(health.stream().filter(c -> c.getWithdrawnAt() == null).count()).isEqualTo(1);
        patchProfile(s, "{\"nickname\":\"성준\"}").andExpect(status().isOk());
    }

    @Test
    void staleConsentWithdrawalCannotRevokeNewerVersion() throws Exception {
        Session s = signupAndVerify();
        changeConsent(s, "PRIVACY", "2026-11", true).andExpect(status().isOk());
        changeConsent(s, "PRIVACY", "2026-10", false).andExpect(status().isConflict());
        patchProfile(s, "{\"nickname\":\"성준\"}").andExpect(status().isOk());
    }

    @Test
    void invalidConsentTypeOrMissingDecisionIsRejected() throws Exception {
        Session s = signupAndVerify();
        changeConsent(s, "UNKNOWN", "v1", true).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/users/me/consents").header("Authorization", "Bearer " + s.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PRIVACY\",\"version\":\"v1\"}"))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(userConsentRepository.count()).isEqualTo(3);
    }

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/users/me/onboarding/complete")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/consents")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/users/me/consents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PRIVACY\",\"version\":\"v1\",\"agreed\":false}")).andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawalDeletesProfileAndOnboardingData() throws Exception {
        Session s = signupAndVerify();
        patchProfile(s, "{\"nickname\":\"성준\",\"heightCm\":175}").andExpect(status().isOk());
        completeOnboarding(s).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + s.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmation\":\"DELETE\"}"))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(profileRepository.count()).isZero();
        patchProfile(s, "{\"nickname\":\"성준\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentConsentAcceptanceCreatesOnlyOneActiveRecord() throws Exception {
        Session s = signupAndVerify();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> call = () -> {
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                return changeConsent(s, "MARKETING", "v1", true).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(call); var second = pool.submit(call); start.countDown();
            org.assertj.core.api.Assertions.assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
            org.assertj.core.api.Assertions.assertThat(second.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
        }
        org.assertj.core.api.Assertions.assertThat(userConsentRepository.count()).isEqualTo(4);
    }

    @Test
    void profileAndConsentChangesAreIsolatedToAuthenticatedUser() throws Exception {
        Session first = signupAndVerify();
        var other = userRepository.save(com.routive.healthhabit.user.domain.User.createSocial(
                "other@example.com", java.time.Instant.now()));
        for (String type : java.util.List.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA")) {
            userConsentRepository.save(com.routive.healthhabit.user.domain.UserConsent.agreed(
                    other, type, "2026-10", java.time.Instant.now()));
        }
        var pair = tokenService.issue(other, java.time.Instant.now());
        Session second = new Session(pair.accessToken(), pair.refreshToken());
        patchProfile(first, "{\"nickname\":\"first\"}").andExpect(status().isOk());
        patchProfile(second, "{\"nickname\":\"second\"}").andExpect(status().isOk());
        changeConsent(second, "PRIVACY", "2026-10", false).andExpect(status().isOk());
        me(first.accessToken()).andExpect(jsonPath("$.data.profile.nickname").value("first"));
        me(second.accessToken()).andExpect(jsonPath("$.data.profile.nickname").value("second"));
        patchProfile(first, "{\"heightCm\":170}").andExpect(status().isOk());
        patchProfile(second, "{\"heightCm\":170}").andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions patchProfile(Session s, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + s.accessToken()).contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private org.springframework.test.web.servlet.ResultActions completeOnboarding(Session s) throws Exception {
        return mockMvc.perform(post("/api/v1/users/me/onboarding/complete").header("Authorization", "Bearer " + s.accessToken()));
    }
    private org.springframework.test.web.servlet.ResultActions changeConsent(Session s, String type, String version, boolean agreed) throws Exception {
        return mockMvc.perform(post("/api/v1/users/me/consents").header("Authorization", "Bearer " + s.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"" + type + "\",\"version\":\"" + version + "\",\"agreed\":" + agreed + "}"));
    }

    private Session signupAndVerify() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson()))
                .andExpect(status().isOk());
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountMailSender).sendVerification(org.mockito.ArgumentMatchers.eq("User@Example.com"), tokenCaptor.capture());
        MvcResult result = mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + tokenCaptor.getValue() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        return new Session(JsonPath.read(json, "$.data.accessToken"), JsonPath.read(json, "$.data.refreshToken"));
    }

    private record Session(String accessToken, String refreshToken) {}

    private String signupJson() {
        return """
                {
                  "email": "User@Example.com",
                  "password": "strong-password-1234",
                  "consents": [
                    {"type": "TERMS_OF_SERVICE", "version": "2026-10", "agreed": true},
                    {"type": "PRIVACY", "version": "2026-10", "agreed": true},
                    {"type": "SENSITIVE_HEALTH_DATA", "version": "2026-10", "agreed": true}
                  ]
                }
                """;
    }
}
