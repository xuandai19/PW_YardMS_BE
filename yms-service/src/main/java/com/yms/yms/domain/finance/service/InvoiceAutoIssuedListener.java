package com.yms.yms.domain.finance.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvoiceAutoIssuedListener {
    private final InvoiceEmailDeliveryService deliveryService;

    public InvoiceAutoIssuedListener(InvoiceEmailDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceIssued(InvoiceAutoIssuedEvent event) {
        deliveryService.deliver(event.invoiceId());
    }
}
