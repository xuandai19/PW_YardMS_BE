package com.yms.yms.domain.finance.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.service.entity.ServiceOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceDocumentService {
    private final Path root;

    public InvoiceDocumentService(@Value("${yms.storage.invoice-path:./data/invoices}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String generate(Invoice invoice, List<ServiceOrder> serviceOrders) {
        Path file = root.resolve(invoice.getId() + ".pdf").normalize();
        if (!file.startsWith(root)) {
            throw new BusinessException("Invalid invoice document path", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            Files.createDirectories(root);
            try (Document document = new Document(new PdfDocument(new PdfWriter(file.toFile())))) {
                document.add(new Paragraph("INVOICE"));
                document.add(new Paragraph("Invoice number: " + invoice.getInvoiceNo()));
                document.add(new Paragraph("Partner: " + invoice.getPartnerId()));
                document.add(new Paragraph("Issue date: " + invoice.getIssueDate().format(DateTimeFormatter.ISO_DATE)));
                document.add(new Paragraph("Due date: " + invoice.getDueDate().format(DateTimeFormatter.ISO_DATE)));
                document.add(new Paragraph("Subtotal: " + invoice.getSubtotal().toPlainString()));
                document.add(new Paragraph("Service orders:"));
                for (ServiceOrder order : serviceOrders) {
                    document.add(new Paragraph("Order " + order.getId() + " · Vehicle "
                            + order.getVehicleId() + " · Service " + order.getServiceId()
                            + " · Quantity " + order.getQuantity().toPlainString()
                            + " · Amount " + order.getTotalAmount().toPlainString()));
                }
                document.add(new Paragraph("Tax: " + invoice.getTaxAmount().toPlainString()));
                document.add(new Paragraph("Total: " + invoice.getTotalAmount().toPlainString()));
            }
            return file.toUri().toString();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate invoice PDF: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path resolve(String fileUri) {
        try {
            Path path = Path.of(java.net.URI.create(fileUri)).toAbsolutePath().normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw new BusinessException("Invoice PDF not found", HttpStatus.NOT_FOUND);
            }
            return path;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invoice PDF not found", HttpStatus.NOT_FOUND);
        }
    }
}
