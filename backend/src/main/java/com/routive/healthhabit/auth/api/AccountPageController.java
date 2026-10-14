package com.routive.healthhabit.auth.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountPageController {
    @GetMapping("/verify-email")
    String verifyEmail() { return "forward:/auth-pages/verify-email.html"; }

    @GetMapping("/reset-password")
    String resetPassword() { return "forward:/auth-pages/reset-password.html"; }
}
