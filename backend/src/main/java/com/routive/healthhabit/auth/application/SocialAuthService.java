package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.auth.api.*; import com.routive.healthhabit.auth.domain.*; import com.routive.healthhabit.auth.social.*;
import com.routive.healthhabit.common.error.*; import com.routive.healthhabit.user.domain.*;
import java.time.Instant; import java.util.*; import java.util.function.Function; import java.util.stream.Collectors;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class SocialAuthService {
    private static final Set<String> REQUIRED = Set.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA");
    private final Map<String, SocialIdentityProvider> providers; private final SocialAccountRepository socials;
    private final UserRepository users; private final UserConsentRepository consents; private final TokenService tokens;
    public SocialAuthService(List<SocialIdentityProvider> providers, SocialAccountRepository socials, UserRepository users,
            UserConsentRepository consents, TokenService tokens) {
        this.providers=providers.stream().collect(Collectors.toMap(SocialIdentityProvider::provider, Function.identity()));
        this.socials=socials; this.users=users; this.consents=consents; this.tokens=tokens;
    }
    public TokenResponse login(SocialLoginRequest request) {
        String providerCode = request.provider().trim().toUpperCase(Locale.ROOT);
        SocialIdentityProvider provider = Optional.ofNullable(providers.get(providerCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        SocialIdentity identity = provider.verify(request.token()); Instant now = Instant.now();
        Optional<SocialAccount> existing = socials.findByProviderAndProviderUserId(providerCode, identity.providerUserId());
        User user;
        if (existing.isPresent()) {
            user = users.findLockedById(existing.get().getUser().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
            if (!socials.existsById(existing.get().getId())) throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            existing.get().markLogin(now);
        }
        else {
            validateConsents(request.consents());
            String email = identity.email() == null ? providerCode.toLowerCase(Locale.ROOT) + "+" + identity.providerUserId() + "@social.invalid" : identity.email();
            if (identity.email() != null && users.existsByEmailNormalized(User.normalizeEmail(email))) throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
            user = users.save(User.createSocial(email, now));
            socials.save(SocialAccount.create(user, providerCode, identity.providerUserId(), identity.email(), now));
            request.consents().stream().filter(SignupRequest.ConsentRequest::agreed)
                    .map(c -> UserConsent.agreed(user, c.type(), c.version(), now)).forEach(consents::save);
        }
        if (!"ACTIVE".equals(user.getStatus())) throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        return TokenResponse.of(user.getId(), tokens.issue(user, now));
    }
    private void validateConsents(List<SignupRequest.ConsentRequest> values) {
        if (values == null) throw new BusinessException(ErrorCode.REQUIRED_CONSENT_MISSING);
        if (values.stream().map(SignupRequest.ConsentRequest::type).distinct().count() != values.size())
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Set<String> agreed = values.stream().filter(SignupRequest.ConsentRequest::agreed).map(SignupRequest.ConsentRequest::type).collect(Collectors.toSet());
        if (!agreed.containsAll(REQUIRED)) throw new BusinessException(ErrorCode.REQUIRED_CONSENT_MISSING);
    }
}
