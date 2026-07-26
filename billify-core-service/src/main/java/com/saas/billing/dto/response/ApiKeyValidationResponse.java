package com.saas.billing.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyValidationResponse {
    private Long userId;
    private Long apiKeyId;
    private boolean active;
}
