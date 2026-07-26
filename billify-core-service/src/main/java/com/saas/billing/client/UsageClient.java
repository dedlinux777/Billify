package com.saas.billing.client;

import com.saas.billing.client.dto.UsageEventRequest;
import com.saas.billing.client.dto.UsageSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "usage-service")
public interface UsageClient {

    @PostMapping("/internal/usage/events")
    void createEvent(@RequestBody UsageEventRequest request);

    @GetMapping("/internal/usage/{userId}")
    UsageSummaryResponse getUsageSummary(@PathVariable("userId") Long userId);
}
