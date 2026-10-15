package com.routive.healthhabit.auth.social;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "social.google")
public record GoogleSocialProperties(String clientId) {}
