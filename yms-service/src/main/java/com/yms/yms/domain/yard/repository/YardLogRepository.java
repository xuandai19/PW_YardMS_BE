package com.yms.yms.domain.yard.repository;

import com.yms.yms.domain.yard.entity.YardLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YardLogRepository extends JpaRepository<YardLog, UUID> {
    boolean existsByPreNotificationIdAndCheckOutAtIsNull(UUID preNotificationId);
    Optional<YardLog> findByPreNotificationIdAndCheckOutAtIsNull(UUID preNotificationId);
    List<YardLog> findAllByOrderByCheckInAtDesc();
}
