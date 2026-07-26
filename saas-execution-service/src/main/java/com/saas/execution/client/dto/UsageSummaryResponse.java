package com.saas.execution.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageSummaryResponse {
    private Long userId;
    private Long invoiceCount;
    private Long apiCallCount;
}
