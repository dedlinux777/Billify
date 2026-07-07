package com.saas.usage.controller;

import com.saas.usage.dto.request.UsageEventRequest;
import com.saas.usage.dto.response.UsageEventResponse;
import com.saas.usage.service.UsageEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/usage/events")
@RequiredArgsConstructor
public class UsageEventController {

    private final UsageEventService usageEventService;
    @Value("${server.port}")
    private String serverPort;

    @PostMapping
    public ResponseEntity<UsageEventResponse> createEvent(@RequestBody UsageEventRequest request) {
        log.info(
                "[INSTANCE {}] POST usage event for user {}",
                serverPort,
                request.userId()
        );
        usageEventService.saveEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsageEventResponse("Usage event recorded successfully"));
    }
}
