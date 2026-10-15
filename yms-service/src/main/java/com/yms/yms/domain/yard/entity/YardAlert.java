package com.yms.yms.domain.yard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "yard_alerts")
@Getter
@Setter
@NoArgsConstructor
public class YardAlert {

    @Id
    private UUID id;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Column(nullable = false)
    private int occupancy;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType = "CAPACITY_90";

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
