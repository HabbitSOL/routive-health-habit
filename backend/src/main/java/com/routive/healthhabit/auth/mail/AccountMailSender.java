package com.routive.healthhabit.auth.mail;
public interface AccountMailSender {
    void sendVerification(String email, String token);
    void sendPasswordReset(String email, String token);
}
