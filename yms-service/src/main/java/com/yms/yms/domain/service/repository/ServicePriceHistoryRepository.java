package com.yms.yms.domain.service.repository;

import com.yms.yms.domain.service.entity.ServicePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServicePriceHistoryRepository extends JpaRepository<ServicePriceHistory, UUID> {
    List<ServicePriceHistory> findByServiceIdOrderByValidFromDesc(UUID serviceId);
}
