package com.routive.healthhabit.user.application;

import com.routive.healthhabit.common.error.BusinessException;
import com.routive.healthhabit.common.error.ErrorCode;
import com.routive.healthhabit.user.api.MeResponse;
import com.routive.healthhabit.user.domain.User;
import com.routive.healthhabit.user.domain.UserConsentRepository;
import com.routive.healthhabit.user.domain.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final com.routive.healthhabit.user.domain.UserProfileRepository profiles;

    public UserQueryService(UserRepository userRepository, UserConsentRepository userConsentRepository,
            com.routive.healthhabit.user.domain.UserProfileRepository profiles) {
        this.userRepository = userRepository;
        this.userConsentRepository = userConsentRepository;
        this.profiles = profiles;
    }

    public MeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.from(user, userConsentRepository.findAllByUserIdOrderByConsentedAtDesc(userId), profiles.findById(userId).orElse(null));
    }
}
