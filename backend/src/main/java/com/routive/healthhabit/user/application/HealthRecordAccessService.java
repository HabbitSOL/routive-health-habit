package com.routive.healthhabit.user.application;

import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.user.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** All callers participate in the outer operation's transaction and user-lock order. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class HealthRecordAccessService {
    private final UserRepository users;
    private final UserConsentRepository consents;
    public HealthRecordAccessService(UserRepository users, UserConsentRepository consents) {
        this.users = users; this.consents = consents;
    }
    public User require(UUID id, boolean write) {
        var user = (write ? users.findLockedById(id) : users.findById(id))
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var active = new HashSet<String>();
        consents.findAllByUserIdOrderByConsentedAtDesc(id).stream()
                .filter(c -> c.getWithdrawnAt() == null).forEach(c -> active.add(c.getConsentType()));
        if (!active.containsAll(Set.of("TERMS_OF_SERVICE", "PRIVACY", "SENSITIVE_HEALTH_DATA")))
            throw new BusinessException(ErrorCode.REQUIRED_CONSENT_MISSING);
        return user;
    }
}
