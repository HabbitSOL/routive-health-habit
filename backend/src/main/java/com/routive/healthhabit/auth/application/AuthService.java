package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.auth.api.LoginRequest;
import com.routive.healthhabit.auth.api.SignupRequest;
import com.routive.healthhabit.auth.api.TokenResponse;
import com.routive.healthhabit.auth.api.SignupResponse;
import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.common.error.ErrorCode;
import com.routive.healthhabit.user.domain.User;
import com.routive.healthhabit.user.domain.UserConsent;
import com.routive.healthhabit.user.domain.UserConsentRepository;
import com.routive.healthhabit.user.domain.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class AuthService {

    private static final Set<String> REQUIRED_CONSENTS = Set.of(
            "TERMS_OF_SERVICE",
            "PRIVACY",
            "SENSITIVE_HEALTH_DATA"
    );

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AccountRecoveryService recoveryService;

    public AuthService(
            UserRepository userRepository,
            UserConsentRepository userConsentRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            AccountRecoveryService recoveryService
    ) {
        this.userRepository = userRepository;
        this.userConsentRepository = userConsentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.recoveryService = recoveryService;
    }

    public SignupResponse signup(SignupRequest request) {
        PasswordPolicy.validate(request.password());
        String normalizedEmail = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        validateRequiredConsents(request);

        Instant now = Instant.now();
        User user = userRepository.save(User.createLocal(
                request.email(),
                passwordEncoder.encode(request.password()),
                now
        ));
        request.consents().stream()
                .filter(SignupRequest.ConsentRequest::agreed)
                .map(consent -> UserConsent.agreed(user, consent.type(), consent.version(), now))
                .forEach(userConsentRepository::save);

        recoveryService.issueVerification(user, now);
        return new SignupResponse(user.getId(), user.getStatus(), true);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findIdByEmail(User.normalizeEmail(request.email()))
                .flatMap(userRepository::findLockedById)
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return TokenResponse.of(user.getId(), tokenService.issue(user, Instant.now()));
    }

    public TokenResponse refresh(String refreshToken) {
        return TokenResponse.from(tokenService.rotate(refreshToken, Instant.now()));
    }

    public void logout(java.util.UUID userId, java.util.UUID sessionId, String refreshToken) {
        tokenService.revoke(refreshToken, userId, sessionId, Instant.now());
    }

    private void validateRequiredConsents(SignupRequest request) {
        if (request.consents().stream().map(SignupRequest.ConsentRequest::type).distinct().count()
                != request.consents().size()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Set<String> agreedTypes = request.consents().stream()
                .filter(SignupRequest.ConsentRequest::agreed)
                .map(SignupRequest.ConsentRequest::type)
                .collect(Collectors.toSet());
        if (!agreedTypes.containsAll(REQUIRED_CONSENTS)) {
            throw new BusinessException(ErrorCode.REQUIRED_CONSENT_MISSING);
        }
    }
}
