package com.saas.execution.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageEventRequest {
    private Long userId;
    private String resourceType;
    private Long apiKeyId;
}
