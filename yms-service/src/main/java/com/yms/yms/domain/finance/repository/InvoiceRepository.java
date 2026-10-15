package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);

    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    List<Invoice> findByPartnerIdOrderByIssueDateDesc(UUID partnerId);
    List<Invoice> findByStatusOrderByIssueDateDesc(String status);
    List<Invoice> findByPartnerIdAndStatusOrderByIssueDateDesc(UUID partnerId, String status);
    List<Invoice> findByPartnerIdAndIssueDateBetweenOrderByIssueDate(UUID partnerId, LocalDate from, LocalDate to);
    List<Invoice> findByIssueDateBetweenOrderByIssueDate(LocalDate from, LocalDate to);
    List<Invoice> findByStatusAndDueDateBefore(String status, LocalDate dueDate);
    List<Invoice> findByPartnerIdInAndIssueDateBetween(List<UUID> partnerIds, LocalDate from, LocalDate to);
}
