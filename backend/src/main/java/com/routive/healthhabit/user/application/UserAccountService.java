package com.routive.healthhabit.user.application;
import com.routive.healthhabit.auth.application.TokenService; import com.routive.healthhabit.auth.domain.SocialAccountRepository;
import com.routive.healthhabit.common.error.*; import com.routive.healthhabit.user.domain.*;
import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class UserAccountService {
    private final UserRepository users; private final UserConsentRepository consents;
    private final SocialAccountRepository socials; private final TokenService tokens;
    private final UserProfileRepository profiles;
    private final com.routive.healthhabit.medication.domain.PersonalMedicationRepository medications;
    public UserAccountService(UserRepository users, UserConsentRepository consents, SocialAccountRepository socials, TokenService tokens, UserProfileRepository profiles,
            com.routive.healthhabit.medication.domain.PersonalMedicationRepository medications) {
        this.users=users; this.consents=consents; this.socials=socials; this.tokens=tokens;
        this.profiles=profiles;
        this.medications=medications;
    }
    public void withdraw(UUID userId) {
        User user = users.findLockedById(userId).filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Instant now = Instant.now(); tokens.revokeAll(userId, now);
        consents.findAllByUserIdOrderByConsentedAtDesc(userId).forEach(c -> c.withdraw(now));
        profiles.findById(userId).ifPresent(profiles::delete);
        medications.deleteAllByUserId(userId);
        socials.deleteAllByUserId(userId); user.withdraw(now);
    }
}
