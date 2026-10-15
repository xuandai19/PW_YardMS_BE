package com.yms.yms.domain.confirmation.repository;

import com.yms.yms.domain.confirmation.entity.DriverConfirmationAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DriverConfirmationAlertRepository extends JpaRepository<DriverConfirmationAlert, UUID> {
    List<DriverConfirmationAlert> findTop100ByOrderByCreatedAtDesc();
}
