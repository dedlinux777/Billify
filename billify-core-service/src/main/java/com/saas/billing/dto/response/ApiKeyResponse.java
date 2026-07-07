package com.saas.billing.dto.response;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {
    private Long id;
    private Long userId;
    private String keyIdentifier;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
