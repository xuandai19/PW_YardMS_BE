package com.yms.yms.domain.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    private UUID id;

    @Column(name = "plate_number", nullable = false, length = 30)
    private String plateNumber;

    @Column(name = "partner_a_id", nullable = false)
    private UUID partnerAId;

    @Column(name = "carrier_company", length = 255)
    private String carrierCompany;

    @Column(name = "vehicle_type", length = 80)
    private String vehicleType;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(nullable = false, length = 30)
    private String status = "REGISTERED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
