package com.saas.billing.invoice;

import com.saas.billing.dto.CreateInvoiceRequestDTO;
import com.saas.billing.dto.InvoiceResponseDTO;
import com.saas.billing.feign.UsageClient;
import com.saas.billing.feign.UsageEventRequestDTO;
import java.time.LocalDateTime;
import com.saas.billing.model.Invoice;
import com.saas.billing.model.InvoiceStatus;
import com.saas.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UsageClient usageClient;

    @Transactional
    public InvoiceResponseDTO createInvoice(CreateInvoiceRequestDTO request) {
        log.info("Creating invoice for user: {}, customer: {}, amount: {}", 
                request.getUserId(), request.getCustomerName(), request.getAmount());

        // 1. Build and save invoice entity locally in core_db
        Invoice invoice = Invoice.builder()
                .userId(request.getUserId())
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .status(InvoiceStatus.GENERATED)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice saved locally with ID: {}", savedInvoice.getId());

        // 2. Call Usage Service through Feign client to log consumption event
        try {
            UsageEventRequestDTO feignRequest = UsageEventRequestDTO.builder()
                    .userId(savedInvoice.getUserId())
                    .resourceType("INVOICE")
                    .build();
            
            log.info("Propagating usage event to usage-service via Feign for user: {}", savedInvoice.getUserId());
            usageClient.createEvent(feignRequest);// creating the event to let usage-service know that an invoice as a event is  created.
            log.info("Usage event successfully recorded by usage-service");
        } catch (Exception e) {
            log.error("Failed to propagate usage event to usage-service: {}", e.getMessage(), e);
            // Optionally rethrow or let transactional logic deal with failure depending on SLA
            throw new RuntimeException("Usage service tracking integration failure. Invoice rollbacked.", e);
        }

        // 3. Return mapped response DTO
        return InvoiceResponseDTO.builder()
                .id(savedInvoice.getId())
                .userId(savedInvoice.getUserId())
                .customerName(savedInvoice.getCustomerName())
                .amount(savedInvoice.getAmount())
                .status(savedInvoice.getStatus())
                .createdAt(savedInvoice.getCreatedAt())
                .build();
    }
}
