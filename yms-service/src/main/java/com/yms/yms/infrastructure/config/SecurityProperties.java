package com.yms.yms.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yms.security")
public record SecurityProperties(String jwtSecret, long expirationSeconds) {
}
