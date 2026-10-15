package com.yms.yms.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_schedules")
@Getter
@Setter
@NoArgsConstructor
public class ReportSchedule {
    @Id
    private UUID id;

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
