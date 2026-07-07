package com.saas.billing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.saas.billing.entity.InvoiceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private BigDecimal amount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}
