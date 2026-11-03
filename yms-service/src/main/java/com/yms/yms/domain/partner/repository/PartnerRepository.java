package com.yms.yms.domain.partner.repository;

import com.yms.yms.domain.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {
    Optional<Partner> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    List<Partner> findByPartnerTypeAndActiveTrue(String partnerType);
    List<Partner> findByActiveTrue();
}
