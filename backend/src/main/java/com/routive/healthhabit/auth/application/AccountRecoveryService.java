package com.routive.healthhabit.auth.application;

import com.routive.healthhabit.auth.domain.*;
import com.routive.healthhabit.auth.mail.AccountMailSender;
import com.routive.healthhabit.common.error.*;
import com.routive.healthhabit.user.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class AccountRecoveryService {
    private final UserRepository users;
    private final EmailVerificationTokenRepository emailTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final TokenService tokenService;
    private final AccountSecurityProperties properties;
    private final AccountMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public AccountRecoveryService(UserRepository users, EmailVerificationTokenRepository emailTokens,
            PasswordResetTokenRepository resetTokens, TokenService tokenService, AccountSecurityProperties properties,
            AccountMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.resetTokens = resetTokens;
        this.tokenService = tokenService;
        this.properties = properties;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public void issueVerification(User user, Instant now) {
        User locked = lockUser(user.getId(), ErrorCode.INVALID_VERIFICATION_TOKEN);
        if (!"PENDING_EMAIL_VERIFICATION".equals(locked.getStatus())) return;
        if (emailTokens.existsByUserIdAndCreatedAtAfter(locked.getId(), now.minusSeconds(properties.mailCooldownSeconds()))) return;
        emailTokens.findAllByUserIdAndUsedAtIsNull(locked.getId()).forEach(token -> token.use(now));
        String raw = tokenService.createOpaqueToken();
        emailTokens.save(EmailVerificationToken.issue(locked, tokenService.hash(raw), now,
                now.plusSeconds(properties.emailTokenSeconds())));
        mailSender.sendVerification(locked.getEmail(), raw);
    }

    public VerifiedEmail verifyEmail(String raw, Instant now) {
        byte[] hash = tokenService.hash(raw);
        UUID userId = emailTokens.findUserIdByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));
        User user = lockUser(userId, ErrorCode.INVALID_VERIFICATION_TOKEN);
        EmailVerificationToken token = emailTokens.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN));
        if (!"PENDING_EMAIL_VERIFICATION".equals(user.getStatus()) || !token.isUsableAt(now)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_TOKEN);
        }
        emailTokens.findAllByUserIdAndUsedAtIsNull(userId).forEach(value -> value.use(now));
        user.verifyEmail(now);
        return new VerifiedEmail(userId, tokenService.issue(user, now));
    }

    public void requestPasswordReset(String email, Instant now) {
        users.findIdByEmail(User.normalizeEmail(email)).ifPresent(id -> {
            User user = lockUser(id, ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
            if (!"ACTIVE".equals(user.getStatus()) || user.getPasswordHash() == null) return;
            if (resetTokens.existsByUserIdAndCreatedAtAfter(id, now.minusSeconds(properties.mailCooldownSeconds()))) return;
            resetTokens.findAllByUserIdAndUsedAtIsNull(id).forEach(token -> token.use(now));
            String raw = tokenService.createOpaqueToken();
            resetTokens.save(PasswordResetToken.issue(user, tokenService.hash(raw), now,
                    now.plusSeconds(properties.passwordResetTokenSeconds())));
            mailSender.sendPasswordReset(user.getEmail(), raw);
        });
    }

    public void resendVerification(String email, Instant now) {
        users.findIdByEmail(User.normalizeEmail(email)).ifPresent(id -> {
            User user = lockUser(id, ErrorCode.INVALID_VERIFICATION_TOKEN);
            issueVerification(user, now);
        });
    }

    public void resetPassword(String raw, String newPassword, Instant now) {
        PasswordPolicy.validate(newPassword);
        byte[] hash = tokenService.hash(raw);
        UUID userId = resetTokens.findUserIdByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        User user = lockUser(userId, ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        PasswordResetToken token = resetTokens.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        if (!"ACTIVE".equals(user.getStatus()) || user.getPasswordHash() == null || !token.isUsableAt(now)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        resetTokens.findAllByUserIdAndUsedAtIsNull(userId).forEach(value -> value.use(now));
        user.changePassword(passwordEncoder.encode(newPassword), now);
        tokenService.revokeAll(userId, now);
    }

    private User lockUser(UUID id, ErrorCode error) {
        return users.findLockedById(id).orElseThrow(() -> new BusinessException(error));
    }

    public record VerifiedEmail(UUID userId, TokenService.IssuedTokenPair tokenPair) {}
}
