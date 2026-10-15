package com.yms.yms.domain.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pre_notifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_pre_notification_plate_day",
                columnNames = {"plate_number", "arrival_date"}))
@Getter
@Setter
@NoArgsConstructor
public class PreNotification {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "partner_a_id", nullable = false)
    private UUID partnerAId;

    @Column(name = "trip_id", nullable = false, unique = true)
    private UUID tripId;

    @Column(name = "plate_number", nullable = false, length = 30)
    private String plateNumber;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "planned_arrival", nullable = false)
    private Instant plannedArrival;

    @Column(name = "driver_name", nullable = false, length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 30)
    private String driverPhone;

    @Column(name = "carrier_company", length = 255)
    private String carrierCompany;

    @Column(name = "vehicle_type", length = 80)
    private String vehicleType;

    @Column(nullable = false, length = 30)
    private String status = "REGISTERED";

    @Column(name = "qr_token", length = 500)
    private String qrToken;

    @Column(name = "qr_expires_at")
    private Instant qrExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "REGISTERED";
        }
    }
}
