package com.yms.yms.domain.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "yard_service_workers")
@Getter
@Setter
@NoArgsConstructor
public class YardServiceWorker {
    @Id
    private UUID id;

    @Column(name = "yard_id", nullable = false)
    private UUID yardId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "worker_name", nullable = false)
    private String workerName;

    @Column(nullable = false)
    private boolean active;
}
