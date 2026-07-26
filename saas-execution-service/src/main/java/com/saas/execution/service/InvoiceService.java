package com.saas.execution.service;

import com.saas.execution.client.ManagementClient;
import com.saas.execution.client.UsageClient;
import com.saas.execution.client.dto.ApiKeyValidationResponse;
import com.saas.execution.client.dto.SubscriptionQuotaResponse;
import com.saas.execution.client.dto.UsageSummaryResponse;
import com.saas.execution.client.dto.UsageEventRequest;
import com.saas.execution.dto.request.CreateInvoiceRequest;
import com.saas.execution.dto.response.InvoiceResponse;
import com.saas.execution.entity.Invoice;
import com.saas.execution.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final ManagementClient managementClient;
    private final UsageClient usageClient;
    private final InvoicePersistenceService invoicePersistenceService;

    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String apiKeyHeader) {
        // 1. Validate API Key & Resolve User via Management service Feign call
        log.info("Performing remote API key validation for key identifier");
        ApiKeyValidationResponse apiKey = managementClient.validateKey(apiKeyHeader);
        Long resolvedUserId = apiKey.getUserId();

        // 2. Fetch Active subscription quota limits via Management service Feign call
        log.info("Performing remote active subscription lookup for user: {}", resolvedUserId);
        SubscriptionQuotaResponse subQuota = managementClient.getActiveSubscriptionQuota(resolvedUserId);

        if (subQuota.isActive()) {
            if (subQuota.getInvoiceLimit() != null || subQuota.getApiCallLimit() != null) {
                try {
                    log.info("Performing remote usage query for user: {}", resolvedUserId);
                    UsageSummaryResponse usage = usageClient.getUsageSummary(resolvedUserId);
                    if (subQuota.getInvoiceLimit() != null && usage.getInvoiceCount() >= subQuota.getInvoiceLimit()) {
                        throw new IllegalStateException("Invoice quota limit exceeded for plan: " + subQuota.getPlanName());
                    }
                    if (subQuota.getApiCallLimit() != null && usage.getApiCallCount() >= subQuota.getApiCallLimit()) {
                        throw new IllegalStateException("API call quota limit exceeded for plan: " + subQuota.getPlanName());
                    }
                } catch (Exception e) {
                    if (e instanceof IllegalStateException) {
                        throw (IllegalStateException) e;
                    }
                    log.warn("Could not retrieve usage summary for quota check: {}", e.getMessage());
                }
            }
        }

        log.info("Orchestrating invoice creation for user: {}, customer: {}, amount: {}", 
                resolvedUserId, request.getCustomerName(), request.getAmount());

        // 3. Delegate to transactional persistence service to save invoice locally in execution_db
        Invoice savedInvoice = invoicePersistenceService.saveInvoice(resolvedUserId, request);

        // 4. Remote Usage Event propagation
        try {
            UsageEventRequest feignRequest = UsageEventRequest.builder()
                    .userId(resolvedUserId)
                    .resourceType("INVOICE")
                    .apiKeyId(apiKey.getApiKeyId())
                    .build();

            log.info("Propagating usage event to usage-service for user: {}", resolvedUserId);
            usageClient.createEvent(feignRequest);
            log.info("Usage event successfully recorded by usage-service");
        } catch (Exception e) {
            log.error("Failed to propagate usage event to usage-service: {}", e.getMessage(), e);
            throw new RuntimeException("Usage service tracking integration failure. Invoice created but event tracking failed.", e);
        }

        // 5. Return mapped response
        return InvoiceMapper.toDTO(savedInvoice);
    }
}
