package com.saas.usage.controller;

import com.saas.usage.dto.UsageSummaryResponse;
import com.saas.usage.model.UsageSummary;
import com.saas.usage.repository.UsageSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
@Slf4j
public class UsageSummaryController {

    private final UsageSummaryRepository usageSummaryRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<UsageSummaryResponse> getUsageSummary(@PathVariable("userId") Long userId) {
        log.info("Fetching usage summary for user: {}", userId);

        UsageSummaryResponse response = usageSummaryRepository.findByUserId(userId)
                .map(summary -> UsageSummaryResponse.builder()
                        .userId(summary.getUserId())
                        .invoiceCount(summary.getInvoiceCount())
                        .apiCallCount(summary.getApiCallCount())
                        .build())
                .orElseGet(() -> UsageSummaryResponse.builder()
                        .userId(userId)
                        .invoiceCount(0L)
                        .apiCallCount(0L)
                        .build());

        return ResponseEntity.ok(response);
    }
}
