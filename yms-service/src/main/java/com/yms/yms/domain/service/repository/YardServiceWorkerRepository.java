package com.yms.yms.domain.service.repository;

import com.yms.yms.domain.service.entity.YardServiceWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface YardServiceWorkerRepository extends JpaRepository<YardServiceWorker, UUID> {
    List<YardServiceWorker> findByYardIdAndServiceTypeAndActiveTrueOrderByWorkerName(UUID yardId, String serviceType);
}
