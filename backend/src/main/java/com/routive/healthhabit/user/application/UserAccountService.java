package com.routive.healthhabit.user.application;
import com.routive.healthhabit.auth.application.TokenService; import com.routive.healthhabit.auth.domain.SocialAccountRepository;
import com.routive.healthhabit.common.error.*; import com.routive.healthhabit.user.domain.*;
import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class UserAccountService {
    private final UserRepository users; private final UserConsentRepository consents;
    private final SocialAccountRepository socials; private final TokenService tokens;
    private final UserProfileRepository profiles;
    private final com.routive.healthhabit.meal.domain.MealRepository meals;
    private final com.routive.healthhabit.meal.domain.MealItemRepository mealItems;
    private final com.routive.healthhabit.medication.domain.PersonalMedicationRepository medications;
    private final com.routive.healthhabit.medication.domain.FixedMedicationScheduleRepository schedules;
    private final com.routive.healthhabit.medication.domain.MedicationOccurrenceRepository occurrences;
    public UserAccountService(UserRepository users, UserConsentRepository consents, SocialAccountRepository socials, TokenService tokens, UserProfileRepository profiles,
            com.routive.healthhabit.medication.domain.PersonalMedicationRepository medications,
            com.routive.healthhabit.medication.domain.FixedMedicationScheduleRepository schedules,
            com.routive.healthhabit.medication.domain.MedicationOccurrenceRepository occurrences,
            com.routive.healthhabit.meal.domain.MealRepository meals,
            com.routive.healthhabit.meal.domain.MealItemRepository mealItems) {
        this.users=users; this.consents=consents; this.socials=socials; this.tokens=tokens;
        this.profiles=profiles;
        this.meals=meals;this.mealItems=mealItems;
        this.medications=medications;
        this.schedules=schedules;this.occurrences=occurrences;
    }
    public void withdraw(UUID userId) {
        User user = users.findLockedById(userId).filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Instant now = Instant.now(); tokens.revokeAll(userId, now);
        consents.findAllByUserIdOrderByConsentedAtDesc(userId).forEach(c -> c.withdraw(now));
        profiles.findById(userId).ifPresent(profiles::delete);
        mealItems.deleteForUser(userId);mealItems.flush();
        meals.deleteForUser(userId);meals.flush();
        occurrences.deleteAllByUserId(userId);occurrences.flush();
        medications.findAllByUserId(userId).forEach(m ->
            schedules.deleteAll(schedules.findAllByMedicationIdOrderByEffectiveFromAsc(m.getId())));
        schedules.flush();
        medications.deleteAllByUserId(userId);
        socials.deleteAllByUserId(userId); user.withdraw(now);
    }
}
