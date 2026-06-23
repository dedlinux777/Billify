package com.saas.usage.dto;

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
public class UsageSummaryResponse {
    private Long userId;
    private Long invoiceCount;
    private Long apiCallCount;
}
