package com.yms.yms.domain.service.repository;

import com.yms.yms.domain.service.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {
    Optional<ServiceEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<ServiceEntity> findByActiveTrue();
}
