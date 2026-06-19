package com.billify.dto;

import com.billify.model.SubscriptionStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionDTO {
    private Long id;
    private String planName;
    private Double planPrice;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}