package com.saas.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.saas.billing.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDTO {
    private Long id;
    private Long userId;
    private String customerName;
    private BigDecimal amount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}
