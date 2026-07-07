package com.saas.billing.service;

import com.saas.billing.client.UsageClient;
import com.saas.billing.client.dto.UsageEventRequest;
import com.saas.billing.dto.request.CreateInvoiceRequest;
import com.saas.billing.entity.ApiKey;
import com.saas.billing.entity.Invoice;
import com.saas.billing.entity.InvoiceStatus;
import com.saas.billing.entity.User;
import com.saas.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePersistenceService {

    private final InvoiceRepository invoiceRepository;
    private final UsageClient usageClient;

    @Transactional
    public Invoice saveInvoiceAndPropagate(User user, ApiKey apiKey, CreateInvoiceRequest request) {
        log.info("Persisting invoice for user: {}, customer: {}", user.getId(), request.getCustomerName());

        Invoice invoice = Invoice.builder()
                .user(user)
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .status(InvoiceStatus.GENERATED)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice saved locally in core_db with ID: {}", savedInvoice.getId());

        try {
            UsageEventRequest feignRequest = UsageEventRequest.builder()
                    .userId(user.getId())
                    .resourceType("INVOICE")
                    .apiKeyId(apiKey.getId())
                    .build();

            log.info("Propagating usage event to usage-service for user: {}", user.getId());
            usageClient.createEvent(feignRequest);
            log.info("Usage event successfully recorded by usage-service");
        } catch (Exception e) {
            log.error("Failed to propagate usage event to usage-service: {}", e.getMessage(), e);
            throw new RuntimeException("Usage service tracking integration failure. Invoice rollbacked.", e);
        }

        return savedInvoice;
    }
}
