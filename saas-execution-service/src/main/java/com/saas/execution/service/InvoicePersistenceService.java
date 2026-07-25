package com.saas.execution.service;

import com.saas.execution.dto.request.CreateInvoiceRequest;
import com.saas.execution.entity.Invoice;
import com.saas.execution.entity.InvoiceStatus;
import com.saas.execution.repository.InvoiceRepository;
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

    @Transactional
    public Invoice saveInvoice(Long userId, CreateInvoiceRequest request) {
        log.info("Persisting invoice for user: {}, customer: {}", userId, request.getCustomerName());

        Invoice invoice = Invoice.builder()
                .userId(userId)
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .status(InvoiceStatus.GENERATED)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice saved locally in execution_db with ID: {}", savedInvoice.getId());

        return savedInvoice;
    }
}
