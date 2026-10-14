package com.routive.healthhabit.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.routive.healthhabit.auth.api.*;
import com.routive.healthhabit.auth.application.*;
import com.routive.healthhabit.auth.domain.*;
import com.routive.healthhabit.auth.social.*;
import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.user.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocialAuthServiceTest {
    private SocialIdentityProvider provider;
    private SocialAccountRepository socials;
    private UserRepository users;
    private UserConsentRepository consents;
    private TokenService tokens;
    private SocialAuthService service;

    @BeforeEach
    void setup() {
        provider = mock(SocialIdentityProvider.class);
        when(provider.provider()).thenReturn("GOOGLE");
        when(provider.verify("valid-token")).thenReturn(new SocialIdentity("GOOGLE", "provider-user-1", "social@example.com", true));
        socials = mock(SocialAccountRepository.class);
        users = mock(UserRepository.class);
        consents = mock(UserConsentRepository.class);
        tokens = mock(TokenService.class);
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.issue(any(), any())).thenReturn(new TokenService.IssuedTokenPair("access", "refresh", 3600));
        service = new SocialAuthService(List.of(provider), socials, users, consents, tokens);
    }

    @Test
    void createsNewUserFromVerifiedSocialIdentity() {
        var result = service.login(request(required()));
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.userId()).isNotNull();
        verify(socials).save(any());
        verify(consents, times(3)).save(any());
    }

    @Test
    void existingSocialLoginNeedsNoRepeatedConsent() {
        User user = User.createSocial("social@example.com", Instant.now());
        SocialAccount account = SocialAccount.create(user, "GOOGLE", "provider-user-1", user.getEmail(), Instant.now());
        when(socials.findByProviderAndProviderUserId("GOOGLE", "provider-user-1")).thenReturn(Optional.of(account));
        when(socials.existsById(account.getId())).thenReturn(true);
        when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));
        assertThat(service.login(request(null)).userId()).isEqualTo(user.getId());
        verify(users, never()).save(any());
    }

    @Test
    void sameEmailDoesNotAutomaticallyLink() {
        when(users.existsByEmailNormalized("social@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.login(request(required()))).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.ACCOUNT_LINK_REQUIRED));
        verify(tokens, never()).issue(any(), any());
    }

    @Test
    void newAccountRequiresConsent() {
        assertThatThrownBy(() -> service.login(request(null))).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.REQUIRED_CONSENT_MISSING));
    }

    @Test
    void deletedUserCannotLoginWithExistingSocialIdentity() {
        User user = User.createSocial("social@example.com", Instant.now());
        SocialAccount account = SocialAccount.create(user, "GOOGLE", "provider-user-1", user.getEmail(), Instant.now());
        user.withdraw(Instant.now());
        when(socials.findByProviderAndProviderUserId("GOOGLE", "provider-user-1")).thenReturn(Optional.of(account));
        when(socials.existsById(account.getId())).thenReturn(true);
        when(users.findLockedById(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.login(request(null))).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(tokens, never()).issue(any(), any());
    }

    @Test
    void unsupportedProviderIsBadRequest() {
        assertThatThrownBy(() -> service.login(new SocialLoginRequest("UNKNOWN", "token", required())))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private SocialLoginRequest request(List<SignupRequest.ConsentRequest> values) {
        return new SocialLoginRequest("google", "valid-token", values);
    }

    private List<SignupRequest.ConsentRequest> required() {
        return List.of(new SignupRequest.ConsentRequest("TERMS_OF_SERVICE", "2026-10", true),
                new SignupRequest.ConsentRequest("PRIVACY", "2026-10", true),
                new SignupRequest.ConsentRequest("SENSITIVE_HEALTH_DATA", "2026-10", true));
    }
}
