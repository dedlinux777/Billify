package com.saas.billing.mapper;

import com.saas.billing.dto.response.ApiKeyResponse;
import com.saas.billing.entity.ApiKey;

public class ApiKeyMapper {

    public static ApiKeyResponse toDTO(ApiKey apiKey) {
        if (apiKey == null) return null;
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .userId(apiKey.getUser().getId())
                .keyIdentifier(apiKey.getKeyIdentifier())
                .active(apiKey.getActive())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .build();
    }
}
