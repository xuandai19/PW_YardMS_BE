package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.finance.repository.InvoiceServiceOrderRepository;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TripInvoiceService {
    private final ServiceOrderRepository serviceOrders;
    private final InvoiceServiceOrderRepository invoiceOrders;
    private final InvoiceRepository invoices;
    private final FinanceService financeService;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TripInvoiceService(ServiceOrderRepository serviceOrders, InvoiceServiceOrderRepository invoiceOrders,
            InvoiceRepository invoices, FinanceService financeService, JdbcTemplate jdbc,
            ApplicationEventPublisher events, Clock clock) {
        this.serviceOrders = serviceOrders;
        this.invoiceOrders = invoiceOrders;
        this.invoices = invoices;
        this.financeService = financeService;
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Invoice issueWhenTripServicesComplete(UUID tripId) {
        List<com.yms.yms.domain.service.entity.ServiceOrder> tripOrders = serviceOrders.findByTripId(tripId);
        if (tripOrders.isEmpty() || tripOrders.stream().anyMatch(order -> !"COMPLETED".equals(order.getStatus()))) {
            return null;
        }
        List<UUID> uninvoicedOrderIds = tripOrders.stream()
                .filter(order -> !invoiceOrders.existsByIdServiceOrderId(order.getId()))
                .map(com.yms.yms.domain.service.entity.ServiceOrder::getId)
                .toList();
        if (uninvoicedOrderIds.isEmpty()) return null;

        List<UUID> partnerIds = jdbc.query("""
                SELECT partner_a_id FROM pre_notifications WHERE trip_id = ?
                """, (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class), tripId);
        if (partnerIds.isEmpty()) return null;

        Invoice invoice = financeService.issueInvoice(new FinanceService.IssueInvoiceRequest(
                partnerIds.get(0), null, uninvoicedOrderIds, LocalDate.now(clock).plusDays(30), BigDecimal.ZERO));
        invoice.setDeliveryStatus("PENDING");
        invoices.save(invoice);
        events.publishEvent(new InvoiceAutoIssuedEvent(invoice.getId()));
        return invoice;
    }
}
