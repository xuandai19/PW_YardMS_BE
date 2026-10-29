package com.yms.yms.domain.finance.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.service.entity.ServiceOrder;
import com.yms.yms.domain.service.repository.ServiceRepository;
import com.yms.yms.domain.vehicle.repository.VehicleRepository;
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
    private static final DeviceRgb NAVY = new DeviceRgb(20, 43, 75);
    private static final DeviceRgb BLUE = new DeviceRgb(36, 99, 180);
    private static final DeviceRgb PALE = new DeviceRgb(241, 246, 252);
    private static final DeviceRgb MUTED = new DeviceRgb(92, 106, 124);
    private final Path root;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;

    public InvoiceDocumentService(@Value("${yms.storage.invoice-path:./data/invoices}") String root,
                                  ServiceRepository serviceRepository, VehicleRepository vehicleRepository) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.serviceRepository = serviceRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public String generate(Invoice invoice, List<ServiceOrder> serviceOrders) {
        Path file = root.resolve(invoice.getId() + ".pdf").normalize();
        if (!file.startsWith(root)) {
            throw new BusinessException("Invalid invoice document path", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            Files.createDirectories(root);
            PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            try (Document document = new Document(new PdfDocument(new PdfWriter(file.toFile())))) {
                document.setMargins(38, 42, 38, 42);

                Table masthead = new Table(UnitValue.createPercentArray(new float[] {62, 38})).useAllAvailableWidth();
                masthead.addCell(new Cell().add(new Paragraph("YMS  |  YARD MANAGEMENT SYSTEM")
                        .setFont(bold).setFontSize(11).setFontColor(BLUE))
                        .setBorder(Border.NO_BORDER).setPadding(0));
                masthead.addCell(new Cell().add(new Paragraph("SERVICE INVOICE")
                        .setFont(bold).setFontSize(22).setFontColor(NAVY)
                        .setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(Border.NO_BORDER).setPadding(0));
                document.add(masthead);
                document.add(new Paragraph("INVOICE  " + invoice.getInvoiceNo())
                        .setFont(bold).setFontSize(10).setFontColor(MUTED).setMarginTop(12).setMarginBottom(18));

                Table summary = new Table(UnitValue.createPercentArray(new float[] {50, 50})).useAllAvailableWidth();
                summary.addCell(infoCell("BILL TO", invoice.getPartnerId().toString(), regular, bold));
                summary.addCell(infoCell("PAYMENT DUE", invoice.getDueDate().format(DateTimeFormatter.ISO_DATE), regular, bold));
                summary.addCell(infoCell("ISSUE DATE", invoice.getIssueDate().format(DateTimeFormatter.ISO_DATE), regular, bold));
                summary.addCell(infoCell("INVOICE STATUS", invoice.getStatus(), regular, bold));
                document.add(summary.setMarginBottom(22));

                Table lines = new Table(UnitValue.createPercentArray(new float[] {10, 31, 19, 18, 22}))
                        .useAllAvailableWidth();
                String[] headings = {"NO.", "SERVICE / ORDER", "VEHICLE", "QUANTITY", "AMOUNT"};
                for (String heading : headings) {
                    lines.addHeaderCell(new Cell().add(new Paragraph(heading).setFont(bold).setFontSize(8)
                            .setFontColor(ColorConstants.WHITE)).setBackgroundColor(NAVY).setBorder(Border.NO_BORDER)
                            .setPadding(8));
                }
                int lineNumber = 1;
                for (ServiceOrder order : serviceOrders) {
                    com.itextpdf.kernel.colors.Color rowColor = lineNumber % 2 == 0 ? PALE : ColorConstants.WHITE;
                    lines.addCell(dataCell(String.format("%02d", lineNumber), regular, rowColor, TextAlignment.CENTER));
                    String serviceName = serviceRepository.findById(order.getServiceId())
                            .map(service -> service.getName() + " (" + service.getCode() + ")")
                            .orElse("Service " + shortId(order.getServiceId()));
                    String plateNumber = vehicleRepository.findById(order.getVehicleId())
                            .map(vehicle -> vehicle.getPlateNumber()).orElse(shortId(order.getVehicleId()));
                    lines.addCell(dataCell(serviceName, regular, rowColor, TextAlignment.LEFT));
                    lines.addCell(dataCell(plateNumber, regular, rowColor, TextAlignment.LEFT));
                    lines.addCell(dataCell(order.getQuantity().stripTrailingZeros().toPlainString(), regular, rowColor, TextAlignment.RIGHT));
                    lines.addCell(dataCell(money(order.getTotalAmount()), regular, rowColor, TextAlignment.RIGHT));
                    lineNumber++;
                }
                document.add(lines);

                Table totals = new Table(UnitValue.createPercentArray(new float[] {60, 20, 20}))
                        .useAllAvailableWidth().setMarginTop(18);
                totals.addCell(new Cell(3, 1).add(new Paragraph("Thank you for using our yard services.")
                        .setFont(regular).setFontSize(9).setFontColor(MUTED))
                        .setBorder(Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
                addTotal(totals, "Subtotal", money(invoice.getSubtotal()), regular, false);
                addTotal(totals, "Tax", money(invoice.getTaxAmount()), regular, false);
                addTotal(totals, "TOTAL DUE", money(invoice.getTotalAmount()), bold, true);
                document.add(totals);
                document.add(new Paragraph("Generated by YMS • Please include the invoice number with your payment.")
                        .setFont(regular).setFontSize(8).setFontColor(MUTED)
                        .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(PALE, 1))
                        .setPaddingTop(10).setMarginTop(28));
            }
            return file.toUri().toString();
        } catch (IOException e) {
            throw new BusinessException("Unable to generate invoice PDF: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Cell infoCell(String title, String value, PdfFont regular, PdfFont bold) {
        return new Cell().add(new Paragraph(title).setFont(bold).setFontSize(8).setFontColor(MUTED))
                .add(new Paragraph(value).setFont(regular).setFontSize(9).setFontColor(NAVY))
                .setBackgroundColor(PALE).setBorder(Border.NO_BORDER).setPadding(10);
    }

    private Cell dataCell(String value, PdfFont font, com.itextpdf.kernel.colors.Color background, TextAlignment alignment) {
        return new Cell().add(new Paragraph(value).setFont(font).setFontSize(8).setFontColor(NAVY))
                .setBackgroundColor(background).setBorder(Border.NO_BORDER).setPadding(8).setTextAlignment(alignment);
    }

    private void addTotal(Table table, String label, String value, PdfFont font, boolean grandTotal) {
        if (grandTotal) {
            table.addCell(new Cell().add(new Paragraph(label).setFont(font).setFontSize(11).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(BLUE).setBorder(Border.NO_BORDER).setPadding(9));
            table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(11).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(BLUE).setBorder(Border.NO_BORDER).setPadding(9));
        } else {
            table.addCell(new Cell().add(new Paragraph(label).setFont(font).setFontSize(9).setFontColor(MUTED))
                    .setBorder(Border.NO_BORDER).setPadding(6));
            table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(9).setFontColor(NAVY)
                    .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER).setPadding(6));
        }
    }

    private String shortId(java.util.UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private String money(java.math.BigDecimal amount) {
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + " VND";
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
