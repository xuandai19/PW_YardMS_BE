package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DebtRepository extends JpaRepository<Debt, UUID> {
    Optional<Debt> findByInvoiceId(UUID invoiceId);
    List<Debt> findByPartnerIdOrderByDueDateAsc(UUID partnerId);
    List<Debt> findByStatusOrderByDueDateAsc(String status);
    List<Debt> findByPartnerIdAndStatusOrderByDueDateAsc(UUID partnerId, String status);
    List<Debt> findByPartnerIdIn(List<UUID> partnerIds);
}
