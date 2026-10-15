package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByInvoiceIdOrderByPaidAtDesc(UUID invoiceId);
    List<Payment> findByInvoiceIdInAndPaidAtBetween(List<UUID> invoiceIds, Instant from, Instant to);
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoiceId = :invoiceId")
    BigDecimal sumForInvoice(@Param("invoiceId") UUID invoiceId);
}
