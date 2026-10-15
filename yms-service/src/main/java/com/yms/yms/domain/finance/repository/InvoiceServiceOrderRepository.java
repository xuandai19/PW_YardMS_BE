package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.InvoiceServiceOrder;
import com.yms.yms.domain.finance.entity.InvoiceServiceOrderId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceServiceOrderRepository extends JpaRepository<InvoiceServiceOrder, InvoiceServiceOrderId> {
    List<InvoiceServiceOrder> findByIdInvoiceId(UUID invoiceId);
    boolean existsByIdServiceOrderId(UUID serviceOrderId);
}
