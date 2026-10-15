package com.yms.yms.domain.confirmation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_confirmations",
        uniqueConstraints = @UniqueConstraint(name = "uq_driver_confirmation_event",
                columnNames = {"trip_id", "confirmation_type"}))
@Getter
@Setter
@NoArgsConstructor
public class DriverConfirmation {
    @Id
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "confirmation_type", nullable = false, length = 20)
    private String confirmationType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "signature_url", length = 1000)
    private String signatureUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
