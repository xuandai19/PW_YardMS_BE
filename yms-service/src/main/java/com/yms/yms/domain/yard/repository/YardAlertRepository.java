package com.yms.yms.domain.yard.repository;

import com.yms.yms.domain.yard.entity.YardAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YardAlertRepository extends JpaRepository<YardAlert, UUID> {
    Optional<YardAlert> findFirstByYardIdAndResolvedAtIsNullOrderByCreatedAtDesc(UUID yardId);
    List<YardAlert> findByResolvedAtIsNullOrderByCreatedAtDesc();
}
