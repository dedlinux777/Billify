package com.saas.billing.dto.response;

import com.saas.billing.entity.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private Long id;
    private Long subscriptionId;
    private String planName;
    private Double amount;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
}
