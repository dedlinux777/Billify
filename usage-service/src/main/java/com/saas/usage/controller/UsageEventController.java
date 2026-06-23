package com.saas.usage.controller;

import com.saas.usage.dto.UsageEventRequestDTO;
import com.saas.usage.dto.UsageEventResponseDTO;
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

    @PostMapping
    public ResponseEntity<UsageEventResponseDTO> createEvent(@RequestBody UsageEventRequestDTO request) {

        usageEventService.saveEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsageEventResponseDTO("Usage event recorded successfully"));
    }
}
