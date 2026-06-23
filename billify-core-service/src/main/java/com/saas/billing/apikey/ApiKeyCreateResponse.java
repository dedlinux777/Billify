package com.saas.billing.apikey;

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
    private String rawApiKey; // The combined format identifier.secret returned once
    private Boolean active;
    private LocalDateTime createdAt;
}
