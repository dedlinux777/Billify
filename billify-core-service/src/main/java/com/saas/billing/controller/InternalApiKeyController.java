package com.saas.billing.controller;

import com.saas.billing.dto.response.ApiKeyValidationResponse;
import com.saas.billing.entity.ApiKey;
import com.saas.billing.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/apikeys")
@RequiredArgsConstructor
@Slf4j
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidationResponse> validateKey(@RequestHeader("X-API-KEY") String apiKey) {
        log.info("Internal request to validate API key");
        ApiKey key = apiKeyService.validateAndRetrieveKey(apiKey);
        ApiKeyValidationResponse response = ApiKeyValidationResponse.builder()
                .userId(key.getUser().getId())
                .apiKeyId(key.getId())
                .active(key.getActive())
                .build();
        return ResponseEntity.ok(response);
    }
}
