package com.saas.billing.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "usage-service", url = "${USAGE_SERVICE_URL:http://localhost:8081}")
public interface UsageClient {

    @PostMapping("/internal/usage/events")
    void createEvent(@RequestBody UsageEventRequestDTO request);

    @GetMapping("/api/usage/{userId}")
    UsageSummaryResponse getUsageSummary(@PathVariable("userId") Long userId);
}

