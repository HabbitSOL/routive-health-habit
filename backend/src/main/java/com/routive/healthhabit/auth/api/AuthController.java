package com.routive.healthhabit.auth.api;

import com.routive.healthhabit.auth.application.AuthService;
import com.routive.healthhabit.auth.application.AccountRecoveryService;
import com.routive.healthhabit.auth.application.SocialAuthService;
import com.routive.healthhabit.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final AccountRecoveryService recoveryService;

    public AuthController(AuthService authService, SocialAuthService socialAuthService, AccountRecoveryService recoveryService) {
        this.authService = authService;
        this.socialAuthService = socialAuthService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/signup")
    ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @PostMapping("/login")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/social")
    ApiResponse<TokenResponse> social(@Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.success(socialAuthService.login(request));
    }

    @PostMapping("/email-verifications/confirm")
    ApiResponse<TokenResponse> verifyEmail(@Valid @RequestBody TokenValueRequest request) {
        AccountRecoveryService.VerifiedEmail verified = recoveryService.verifyEmail(request.token(), java.time.Instant.now());
        return ApiResponse.success(TokenResponse.of(verified.userId(), verified.tokenPair()));
    }

    @PostMapping("/email-verifications/resend")
    ApiResponse<java.util.Map<String, Boolean>> resendVerification(@Valid @RequestBody EmailRequest request) {
        recoveryService.resendVerification(request.email(), java.time.Instant.now());
        return ApiResponse.success(java.util.Map.of("accepted", true));
    }

    @PostMapping("/password-resets/request")
    ApiResponse<java.util.Map<String, Boolean>> requestReset(@Valid @RequestBody EmailRequest request) {
        recoveryService.requestPasswordReset(request.email(), java.time.Instant.now());
        return ApiResponse.success(java.util.Map.of("accepted", true));
    }

    @PostMapping("/password-resets/confirm")
    ApiResponse<java.util.Map<String, Boolean>> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        recoveryService.resetPassword(request.token(), request.newPassword(), java.time.Instant.now());
        return ApiResponse.success(java.util.Map.of("completed", true));
    }

    @PostMapping("/logout")
    ApiResponse<java.util.Map<String, Boolean>> logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest request) {
        authService.logout(java.util.UUID.fromString(jwt.getSubject()),
                java.util.UUID.fromString(jwt.getClaimAsString("sid")), request.refreshToken());
        return ApiResponse.success(java.util.Map.of("loggedOut", true));
    }
}
