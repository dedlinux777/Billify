package com.saas.usage.service;

import com.saas.usage.dto.request.UsageEventRequest;
import com.saas.usage.entity.UsageEvent;
import com.saas.usage.entity.UsageSummary;
import com.saas.usage.repository.UsageEventRepository;
import com.saas.usage.repository.UsageSummaryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageEventService {

    private final UsageEventRepository usageEventRepository;
    private final UsageSummaryRepository usageSummaryRepository;



    @Transactional
    public void saveEvent(UsageEventRequest request) {
        log.info("Saving usage event for user: {}, resource: {}, apiKeyId: {}", 
                request.getUserId(), request.getResourceType(), request.getApiKeyId());
        
        UsageEvent event = UsageEvent.builder()
                .userId(request.getUserId())
                .resourceType(request.getResourceType())
                .apiKeyId(request.getApiKeyId())
                .createdAt(LocalDateTime.now())
                .build();

        usageEventRepository.save(event);
        log.info("Usage event saved successfully in database");

        if (request.getResourceType() != null && "INVOICE".equalsIgnoreCase(request.getResourceType())) {
            UsageSummary summary = usageSummaryRepository.findByUserId(request.getUserId())
                    .orElseGet(() -> UsageSummary.builder()
                            .userId(request.getUserId())
                            .invoiceCount(0L)
                            .apiCallCount(0L)
                            .build());

            summary.setInvoiceCount(summary.getInvoiceCount() + 1);
            summary.setApiCallCount(summary.getApiCallCount() + 1);
            usageSummaryRepository.save(summary);
            log.info("Updated usage summary for user {}: invoiceCount={}, apiCallCount={}", 
                    request.getUserId(), summary.getInvoiceCount(), summary.getApiCallCount());
        }
    }
}
