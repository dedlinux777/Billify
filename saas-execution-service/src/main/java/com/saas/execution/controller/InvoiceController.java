package com.saas.execution.controller;

import com.saas.execution.dto.request.CreateInvoiceRequest;
import com.saas.execution.dto.response.InvoiceResponse;
import com.saas.execution.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader("X-API-KEY") String apiKeyHeader) {
        InvoiceResponse response = invoiceService.createInvoice(request, apiKeyHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
