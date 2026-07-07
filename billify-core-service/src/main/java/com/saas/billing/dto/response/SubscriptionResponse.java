package com.saas.billing.dto.response;

import com.saas.billing.entity.SubscriptionStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionResponse {
    private Long id;
    private String planName;
    private Double planPrice;
    private String rawApiKey;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
