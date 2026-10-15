package com.yms.yms.domain.finance.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.finance.entity.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EInvoiceGateway {
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;

    public EInvoiceGateway(@Value("${yms.finance.e-invoice.enabled:false}") boolean enabled,
            @Value("${yms.finance.e-invoice.base-url:}") String baseUrl,
            @Value("${yms.finance.e-invoice.api-key:}") String apiKey,
            RestClient.Builder restClientBuilder) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.build();
    }

    public String submit(Invoice invoice) {
        if (!enabled || baseUrl.isBlank() || apiKey.isBlank()) {
            throw new BusinessException("E-invoice provider is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        EInvoiceResponse response = restClient.post()
                .uri(baseUrl + "/invoices")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(new EInvoiceRequest(invoice.getInvoiceNo(), invoice.getPartnerId(),
                        invoice.getIssueDate(), invoice.getTotalAmount(), invoice.getTaxAmount()))
                .retrieve()
                .body(EInvoiceResponse.class);
        if (response == null || response.providerInvoiceId() == null || response.providerInvoiceId().isBlank()) {
            throw new BusinessException("E-invoice provider returned an invalid response",
                    HttpStatus.BAD_GATEWAY);
        }
        return response.providerInvoiceId();
    }

    public record EInvoiceRequest(String invoiceNo, java.util.UUID partnerId,
            java.time.LocalDate issueDate, java.math.BigDecimal totalAmount, java.math.BigDecimal taxAmount) {
    }

    public record EInvoiceResponse(String providerInvoiceId) {
    }
}
