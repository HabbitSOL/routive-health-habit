package com.routive.healthhabit.auth.social;
public interface SocialIdentityProvider {
    String provider();
    SocialIdentity verify(String token);
}
