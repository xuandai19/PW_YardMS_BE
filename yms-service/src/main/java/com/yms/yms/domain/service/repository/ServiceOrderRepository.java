package com.yms.yms.domain.service.repository;

import com.yms.yms.domain.service.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {
    List<ServiceOrder> findByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);
    List<ServiceOrder> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByTripIdAndServiceId(UUID tripId, UUID serviceId);
}
