package com.yms.yms.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory refresh-token store for foundation sprint.
 * Can be replaced by Redis without changing AuthService contract.
 */
@Component
public class TokenRepository {

    private record Entry(UUID userId, Instant expiresAt) {
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public void save(String refreshToken, UUID userId, long ttlSeconds) {
        tokens.put(refreshToken, new Entry(userId, Instant.now().plusSeconds(ttlSeconds)));
    }

    public UUID consume(String refreshToken) {
        Entry entry = tokens.remove(refreshToken);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.userId();
    }

    public void revoke(String refreshToken) {
        tokens.remove(refreshToken);
    }
}
