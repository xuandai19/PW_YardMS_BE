package com.yms.yms.domain.confirmation.repository;

import com.yms.yms.domain.confirmation.entity.DriverConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DriverConfirmationRepository extends JpaRepository<DriverConfirmation, UUID> {
    List<DriverConfirmation> findByTripIdOrderByRequestedAt(UUID tripId);
    List<DriverConfirmation> findByStatusAndDueAtBefore(String status, Instant dueAt);
}
