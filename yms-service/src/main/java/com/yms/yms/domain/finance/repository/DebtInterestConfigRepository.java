package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.DebtInterestConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DebtInterestConfigRepository extends JpaRepository<DebtInterestConfig, UUID> {
    Optional<DebtInterestConfig> findByPartnerId(UUID partnerId);
}
