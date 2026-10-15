package com.routive.healthhabit.user.api;

import com.routive.healthhabit.user.domain.UserProfile;
import java.math.BigDecimal;
import java.time.*;

public record ProfileResponse(String nickname, LocalDate birthDate, String sex, BigDecimal heightCm,
                              Instant onboardingCompletedAt) {
    public static ProfileResponse from(UserProfile profile) {
        if (profile == null) return new ProfileResponse(null, null, null, null, null);
        return new ProfileResponse(profile.getNickname(), profile.getBirthDate(), profile.getSex(),
                profile.getHeightCm(), profile.getOnboardingCompletedAt());
    }
}
