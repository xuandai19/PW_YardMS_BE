package com.yms.yms.domain.tracking.repository;

import com.yms.yms.domain.tracking.entity.RouteAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteAlertRepository extends JpaRepository<RouteAlert, UUID> {
    List<RouteAlert> findTop100ByOrderByCreatedAtDesc();
}
