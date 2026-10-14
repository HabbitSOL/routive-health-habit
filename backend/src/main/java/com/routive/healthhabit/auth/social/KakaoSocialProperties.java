package com.routive.healthhabit.auth.social;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "social.kakao")
public record KakaoSocialProperties(String appId) {}
