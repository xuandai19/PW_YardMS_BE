package com.yms.yms.domain.vehicle.repository;

import com.yms.yms.domain.vehicle.entity.PreNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreNotificationRepository extends JpaRepository<PreNotification, UUID> {
    boolean existsByPlateNumberAndArrivalDate(String plateNumber, LocalDate arrivalDate);
    List<PreNotification> findByStatusOrderByPlannedArrivalAsc(String status);
    Optional<PreNotification> findByQrToken(String qrToken);
}
