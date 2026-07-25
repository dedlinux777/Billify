package com.saas.execution.client;

import com.saas.execution.client.dto.ApiKeyValidationResponse;
import com.saas.execution.client.dto.SubscriptionQuotaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "billify-core-service")
public interface ManagementClient {

    @PostMapping("/internal/apikeys/validate")
    ApiKeyValidationResponse validateKey(@RequestHeader("X-API-KEY") String apiKey);

    @GetMapping("/internal/subscriptions/active-quota")
    SubscriptionQuotaResponse getActiveSubscriptionQuota(@RequestParam("userId") Long userId);
}
