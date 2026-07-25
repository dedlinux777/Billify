package com.saas.execution.mapper;

import com.saas.execution.dto.response.InvoiceResponse;
import com.saas.execution.entity.Invoice;

public class InvoiceMapper {

    public static InvoiceResponse toDTO(Invoice invoice) {
        if (invoice == null) return null;
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .userId(invoice.getUserId())
                .customerName(invoice.getCustomerName())
                .amount(invoice.getAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
