package com.saas.billing.service;

import com.saas.billing.client.UsageClient;
import com.saas.billing.client.dto.UsageSummaryResponse;
import com.saas.billing.dto.request.CreateInvoiceRequest;
import com.saas.billing.dto.response.InvoiceResponse;
import com.saas.billing.entity.ApiKey;
import com.saas.billing.entity.Invoice;
import com.saas.billing.entity.Plan;
import com.saas.billing.entity.SubscriptionStatus;
import com.saas.billing.entity.User;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.mapper.InvoiceMapper;
import com.saas.billing.repository.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final UsageClient usageClient;
    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoicePersistenceService invoicePersistenceService;

    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String apiKeyHeader) {
        // 1. Validate API Key & Resolve User (updates api key lastUsedAt in its own transaction)
        ApiKey apiKey = apiKeyService.validateAndRetrieveKey(apiKeyHeader);
        User user = apiKey.getUser();
        Long resolvedUserId = user.getId();

        // 2. Fetch User and enforce Plan Quota Limits
        User dbUser = userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + resolvedUserId));

        subscriptionRepository.findByUserAndStatus(dbUser, SubscriptionStatus.ACTIVE)
                .ifPresent(subscription -> {
                    Plan plan = subscription.getPlan();
                    if (plan.getInvoiceLimit() != null || plan.getApiCallLimit() != null) {
                        try {
                            log.info("Performing remote usage query (outside transactional scope) for user: {}", resolvedUserId);
                            UsageSummaryResponse usage = usageClient.getUsageSummary(resolvedUserId);
                            if (plan.getInvoiceLimit() != null && usage.getInvoiceCount() >= plan.getInvoiceLimit()) {
                                throw new IllegalStateException("Invoice quota limit exceeded for plan: " + plan.getName());
                            }
                            if (plan.getApiCallLimit() != null && usage.getApiCallCount() >= plan.getApiCallLimit()) {
                                throw new IllegalStateException("API call quota limit exceeded for plan: " + plan.getName());
                            }
                        } catch (Exception e) {
                            if (e instanceof IllegalStateException) {
                                throw (IllegalStateException) e;
                            }
                            log.warn("Could not retrieve usage summary for quota check: {}", e.getMessage());
                        }
                    }
                });

        log.info("Orchestrating invoice creation for user: {}, customer: {}, amount: {}", 
                resolvedUserId, request.getCustomerName(), request.getAmount());

        // 3. Delegate to transactional persistence service to save invoice locally and propagate event
        Invoice savedInvoice = invoicePersistenceService.saveInvoiceAndPropagate(dbUser, apiKey, request);

        // 4. Return mapped response
        return InvoiceMapper.toDTO(savedInvoice);
    }
}
