package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.partner.repository.PartnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class InvoiceEmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailDeliveryService.class);

    private final InvoiceRepository invoices;
    private final PartnerRepository partners;
    private final InvoiceDocumentService documents;
    private final JavaMailSender mailSender;
    private final Clock clock;

    public InvoiceEmailDeliveryService(InvoiceRepository invoices, PartnerRepository partners,
            InvoiceDocumentService documents, org.springframework.beans.factory.ObjectProvider<JavaMailSender> sender,
            Clock clock) {
        this.invoices = invoices;
        this.partners = partners;
        this.documents = documents;
        this.mailSender = sender.getIfAvailable();
        this.clock = clock;
    }

    @Transactional
    public void deliver(UUID invoiceId) {
        var invoice = invoices.findById(invoiceId).orElse(null);
        if (invoice == null) return;
        try {
            var partner = partners.findById(invoice.getPartnerId()).orElse(null);
            if (partner == null || partner.getContactEmail() == null || partner.getContactEmail().isBlank()) {
                throw new IllegalStateException("Đối tác chưa có email liên hệ");
            }
            if (mailSender == null) {
                throw new IllegalStateException("Chưa cấu hình máy chủ gửi email");
            }

            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(partner.getContactEmail());
            helper.setSubject("Hóa đơn dịch vụ bãi xe " + invoice.getInvoiceNo());
            helper.setText("Kính gửi " + partner.getName() + ",\n\n"
                    + "Hóa đơn " + invoice.getInvoiceNo() + " cho dịch vụ xe tại bãi đã được phát hành.\n"
                    + "Tổng tiền: " + invoice.getTotalAmount().toPlainString() + "\n"
                    + "Hạn thanh toán: " + invoice.getDueDate() + "\n"
                    + "Vui lòng xem hóa đơn PDF đính kèm.\n");
            helper.addAttachment(invoice.getInvoiceNo() + ".pdf",
                    new FileSystemResource(documents.resolve(invoice.getPdfUrl())));
            mailSender.send(message);
            invoice.setDeliveryStatus("SENT");
            invoice.setDeliveryError(null);
            invoice.setDeliveredAt(clock.instant());
        } catch (Exception exception) {
            log.warn("Could not email auto-issued invoice {}: {}", invoiceId, exception.getMessage());
            invoice.setDeliveryStatus("FAILED");
            invoice.setDeliveryError(exception.getMessage() == null ? "Email delivery failed"
                    : exception.getMessage().substring(0, Math.min(exception.getMessage().length(), 500)));
            invoice.setDeliveredAt(Instant.now(clock));
        }
        invoices.save(invoice);
    }
}
