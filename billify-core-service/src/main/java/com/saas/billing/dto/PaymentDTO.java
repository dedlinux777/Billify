package com.saas.billing.dto;

import com.saas.billing.model.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDTO {
    private Long id;
    private Long subscriptionId;
    private String planName;
    private Double amount;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
}