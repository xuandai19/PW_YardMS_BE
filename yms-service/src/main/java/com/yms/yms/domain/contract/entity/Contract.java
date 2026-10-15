package com.yms.yms.domain.contract.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class Contract {

    @Id
    private UUID id;

    @Column(name = "contract_no", nullable = false, unique = true, length = 80)
    private String contractNo;

    @Column(name = "contract_type", nullable = false, length = 10)
    private String contractType;

    @Column(name = "partner_a_id", nullable = false)
    private UUID partnerAId;

    @Column(name = "partner_c_id")
    private UUID partnerCId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "contract_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal contractValue;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

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
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
