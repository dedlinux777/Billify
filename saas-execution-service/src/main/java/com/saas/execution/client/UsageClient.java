package com.saas.execution.client;

import com.saas.execution.client.dto.UsageEventRequest;
import com.saas.execution.client.dto.UsageSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "usage-service")
public interface UsageClient {

    @PostMapping("/internal/usage/events")
    void createEvent(@RequestBody UsageEventRequest request);

    @GetMapping("/internal/usage/{userId}")
    UsageSummaryResponse getUsageSummary(@PathVariable("userId") Long userId);
}
