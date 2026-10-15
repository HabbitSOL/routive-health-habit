package com.routive.healthhabit.user.application;

import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.user.api.*;
import com.routive.healthhabit.user.domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UserProfileService {
    private static final Set<String> REQUIRED = Set.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA");
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final UserConsentRepository consents;

    public UserProfileService(UserRepository users, UserProfileRepository profiles, UserConsentRepository consents) {
        this.users = users; this.profiles = profiles; this.consents = consents;
    }

    public ProfileResponse patch(UUID id, ProfilePatchRequest request) {
        User user = lockActive(id);
        requireConsents(id);
        Set<String> clear = request.clearFields() == null ? Set.of() : request.clearFields();
        if ((clear.contains("birthDate") && request.birthDate() != null)
                || (clear.contains("sex") && request.sex() != null)
                || (clear.contains("heightCm") && request.heightCm() != null)
                || (request.nickname() != null && request.nickname().strip().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Instant now = Instant.now();
        UserProfile profile = profiles.findById(id).orElseGet(() -> new UserProfile(user, now));
        profile.update(request.nickname() == null ? profile.getNickname() : request.nickname().strip(),
                clear.contains("birthDate") ? null : request.birthDate() == null ? profile.getBirthDate() : request.birthDate(),
                clear.contains("sex") ? null : request.sex() == null ? profile.getSex() : request.sex(),
                clear.contains("heightCm") ? null : request.heightCm() == null ? profile.getHeightCm() : request.heightCm(), now);
        return ProfileResponse.from(profiles.save(profile));
    }

    public ProfileResponse completeOnboarding(UUID id) {
        lockActive(id);
        requireConsents(id);
        UserProfile profile = profiles.findById(id).filter(p -> p.getNickname() != null && !p.getNickname().isBlank())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        profile.completeOnboarding(Instant.now());
        return ProfileResponse.from(profile);
    }

    public List<MeResponse.Consent> changeConsent(UUID id, ConsentChangeRequest request) {
        User user = lockActive(id);
        List<UserConsent> history = consents.findAllByUserIdOrderByConsentedAtDesc(id);
        List<UserConsent> active = history.stream().filter(c -> c.getConsentType().equals(request.type())
                && c.getWithdrawnAt() == null).toList();
        Instant now = Instant.now();
        if (request.agreed()) {
            if (active.stream().noneMatch(c -> c.getVersion().equals(request.version()))) {
                active.forEach(c -> c.withdraw(now));
                consents.save(UserConsent.agreed(user, request.type(), request.version(), now));
            }
        } else {
            // A stale screen must not withdraw a newer version the user has just accepted.
            if (active.stream().anyMatch(c -> !c.getVersion().equals(request.version()))) {
                throw new BusinessException(ErrorCode.DATA_CONFLICT);
            }
            active.forEach(c -> c.withdraw(now));
        }
        return consentHistory(id);
    }

    @Transactional(readOnly = true)
    public List<MeResponse.Consent> consentHistory(UUID id) {
        users.findById(id).filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return consents.findAllByUserIdOrderByConsentedAtDesc(id).stream().map(MeResponse.Consent::from).toList();
    }

    private User lockActive(UUID id) {
        return users.findLockedById(id).filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void requireConsents(UUID id) {
        Set<String> active = new HashSet<>();
        consents.findAllByUserIdOrderByConsentedAtDesc(id).stream().filter(c -> c.getWithdrawnAt() == null)
                .forEach(c -> active.add(c.getConsentType()));
        if (!active.containsAll(REQUIRED)) throw new BusinessException(ErrorCode.REQUIRED_CONSENT_MISSING);
    }
}
