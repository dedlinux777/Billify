package com.saas.billing.dto.response;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyCreateResponse {
    private Long id;
    private Long userId;
    private String keyIdentifier;
    private String rawApiKey;
    private Boolean active;
    private LocalDateTime createdAt;
}
