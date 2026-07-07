package com.saas.billing.mapper;

import com.saas.billing.dto.response.InvoiceResponse;
import com.saas.billing.entity.Invoice;

public class InvoiceMapper {

    public static InvoiceResponse toDTO(Invoice invoice) {
        if (invoice == null) return null;
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .userId(invoice.getUser().getId())
                .customerName(invoice.getCustomerName())
                .amount(invoice.getAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
