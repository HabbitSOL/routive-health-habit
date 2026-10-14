package com.routive.healthhabit.auth.social;
public record SocialIdentity(String provider, String providerUserId, String email, boolean emailVerified) {}
