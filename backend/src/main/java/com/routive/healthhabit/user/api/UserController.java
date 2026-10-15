package com.routive.healthhabit.user.api;

import com.routive.healthhabit.common.api.ApiResponse;
import com.routive.healthhabit.user.application.UserQueryService;
import com.routive.healthhabit.user.application.UserAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserQueryService userQueryService;
    private final UserAccountService userAccountService;
    private final com.routive.healthhabit.user.application.UserProfileService profiles;

    public UserController(UserQueryService userQueryService, UserAccountService userAccountService,
            com.routive.healthhabit.user.application.UserProfileService profiles) {
        this.userQueryService = userQueryService;
        this.userAccountService = userAccountService;
        this.profiles = profiles;
    }

    @GetMapping("/me")
    ApiResponse<MeResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userQueryService.getMe(UUID.fromString(jwt.getSubject())));
    }

    @DeleteMapping("/me")
    ApiResponse<Map<String, Boolean>> withdraw(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WithdrawRequest request) {
        userAccountService.withdraw(UUID.fromString(jwt.getSubject()));
        return ApiResponse.success(Map.of("withdrawn", true));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me")
    ApiResponse<ProfileResponse> patch(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfilePatchRequest request) {
        return ApiResponse.success(profiles.patch(UUID.fromString(jwt.getSubject()), request));
    }

    @org.springframework.web.bind.annotation.PostMapping("/me/onboarding/complete")
    ApiResponse<ProfileResponse> completeOnboarding(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(profiles.completeOnboarding(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/me/consents")
    ApiResponse<java.util.List<MeResponse.Consent>> consents(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(profiles.consentHistory(UUID.fromString(jwt.getSubject())));
    }

    @org.springframework.web.bind.annotation.PostMapping("/me/consents")
    ApiResponse<java.util.List<MeResponse.Consent>> changeConsent(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConsentChangeRequest request) {
        return ApiResponse.success(profiles.changeConsent(UUID.fromString(jwt.getSubject()), request));
    }
}
