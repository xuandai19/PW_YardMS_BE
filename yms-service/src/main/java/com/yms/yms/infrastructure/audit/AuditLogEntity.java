package com.yms.yms.infrastructure.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLogEntity {
    @Id
    private UUID id;
    @Column(name = "actor_username", nullable = false, length = 80)
    private String actorUsername;
    @Column(nullable = false, length = 50)
    private String module;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "request_path", length = 255)
    private String requestPath;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(columnDefinition = "text")
    private String error;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AuditLogEntity from(String actorUsername, String module, String action,
                                      String status, String requestPath, long durationMs, String error) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = UUID.randomUUID();
        entity.actorUsername = actorUsername;
        entity.module = module;
        entity.action = action;
        entity.status = status;
        entity.requestPath = requestPath;
        entity.durationMs = durationMs;
        entity.error = error;
        entity.createdAt = Instant.now();
        return entity;
    }
}
