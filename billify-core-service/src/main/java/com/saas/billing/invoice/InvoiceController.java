package com.saas.billing.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponseDTO> createInvoice(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestBody CreateInvoiceRequestDTO request) {
        InvoiceResponseDTO response = invoiceService.createInvoice(request, apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
