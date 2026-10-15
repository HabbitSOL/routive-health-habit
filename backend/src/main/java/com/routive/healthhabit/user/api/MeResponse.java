package com.routive.healthhabit.user.api;

import com.routive.healthhabit.user.domain.User;
import com.routive.healthhabit.user.domain.UserConsent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        String status,
        String timezone,
        String locale,
        List<Consent> consents,
        ProfileResponse profile
) {

    public static MeResponse from(User user, List<UserConsent> consents, com.routive.healthhabit.user.domain.UserProfile profile) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.getTimezone(),
                user.getLocale(),
                consents.stream().map(Consent::from).toList(),
                ProfileResponse.from(profile)
        );
    }

    public record Consent(String type, String version, Instant consentedAt, Instant withdrawnAt) {
        public static Consent from(UserConsent consent) {
            return new Consent(
                    consent.getConsentType(),
                    consent.getVersion(),
                    consent.getConsentedAt(),
                    consent.getWithdrawnAt()
            );
        }
    }
}
