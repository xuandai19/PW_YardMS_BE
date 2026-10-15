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
@Table(name = "yard_vehicle_assignments")
@Getter
@Setter
@NoArgsConstructor
public class YardVehicleAssignment {

    @Id
    private UUID id;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(nullable = false, length = 20)
    private String status = "IN_YARD";

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = "IN_YARD";
        }
    }
}
