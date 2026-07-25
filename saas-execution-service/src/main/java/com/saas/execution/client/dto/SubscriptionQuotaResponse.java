package com.saas.execution.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionQuotaResponse {
    private Long subscriptionId;
    private String planName;
    private Long invoiceLimit;
    private Long apiCallLimit;
    private boolean active;
}
