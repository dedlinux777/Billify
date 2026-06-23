package com.saas.billing.invoice;

import com.saas.billing.model.ApiKey;
import com.saas.billing.apikey.ApiKeyService;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.feign.UsageClient;
import com.saas.billing.feign.UsageEventRequestDTO;
import com.saas.billing.feign.UsageSummaryResponse;
import com.saas.billing.model.Invoice;
import com.saas.billing.model.InvoiceStatus;
import com.saas.billing.model.Plan;
import com.saas.billing.model.SubscriptionStatus;
import com.saas.billing.model.User;
import com.saas.billing.subscription.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UsageClient usageClient;
    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public InvoiceResponseDTO createInvoice(CreateInvoiceRequestDTO request, String apiKeyHeader) {
        // 1. Validate API Key & Resolve User
        ApiKey apiKey = apiKeyService.validateAndRetrieveKey(apiKeyHeader);
        Long resolvedUserId = apiKey.getUserId();

        // 2. Fetch User and enforce Plan Quota Limits
        User user = userRepository.findById(resolvedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + resolvedUserId));

        subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .ifPresent(subscription -> {
                    Plan plan = subscription.getPlan();
                    if (plan.getInvoiceLimit() != null || plan.getApiCallLimit() != null) {
                        try {
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

        log.info("Creating invoice for user: {}, customer: {}, amount: {}", 
                resolvedUserId, request.getCustomerName(), request.getAmount());

        // 3. Build and save invoice entity locally in core_db
        Invoice invoice = Invoice.builder()
                .userId(resolvedUserId)
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .status(InvoiceStatus.GENERATED)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice saved locally with ID: {}", savedInvoice.getId());

        // 4. Call Usage Service through Feign client to log consumption event
        try {
            UsageEventRequestDTO feignRequest = UsageEventRequestDTO.builder()
                    .userId(savedInvoice.getUserId())
                    .resourceType("INVOICE")
                    .apiKeyId(apiKey.getId())
                    .build();
            
            log.info("Propagating usage event to usage-service via Feign for user: {}", savedInvoice.getUserId());
            usageClient.createEvent(feignRequest);
            log.info("Usage event successfully recorded by usage-service");
        } catch (Exception e) {
            log.error("Failed to propagate usage event to usage-service: {}", e.getMessage(), e);
            throw new RuntimeException("Usage service tracking integration failure. Invoice rollbacked.", e);
        }

        // 5. Return mapped response DTO
        return InvoiceResponseDTO.builder()
                .id(savedInvoice.getId())
                .userId(savedInvoice.getUserId())
                .customerName(savedInvoice.getCustomerName())
                .amount(savedInvoice.getAmount())
                .status(savedInvoice.getStatus())
                .createdAt(savedInvoice.getCreatedAt())
                .build();
    }
}
