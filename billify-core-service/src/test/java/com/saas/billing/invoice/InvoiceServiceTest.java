package com.saas.billing.invoice;

import com.saas.billing.model.ApiKey;
import com.saas.billing.apikey.ApiKeyService;
import com.saas.billing.feign.UsageClient;
import com.saas.billing.feign.UsageSummaryResponse;
import com.saas.billing.model.Plan;
import com.saas.billing.model.Role;
import com.saas.billing.model.Subscription;
import com.saas.billing.model.SubscriptionStatus;
import com.saas.billing.model.User;
import com.saas.billing.subscription.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UsageClient usageClient;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User user;
    private ApiKey apiKey;
    private Plan plan;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();

        apiKey = ApiKey.builder()
                .id(100L)
                .userId(1L)
                .keyIdentifier("blfy_id_test")
                .keyHash("hashed_secret")
                .active(true)
                .build();

        plan = Plan.builder()
                .id(10L)
                .name("Pro Plan")
                .invoiceLimit(3L)
                .apiCallLimit(10L)
                .build();

        subscription = Subscription.builder()
                .id(50L)
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    @Test
    void testCreateInvoice_Success() {
        CreateInvoiceRequestDTO request = CreateInvoiceRequestDTO.builder()
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .build();

        when(apiKeyService.validateAndRetrieveKey("raw_api_key")).thenReturn(apiKey);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(subscription));

        // Mock usage within limit
        UsageSummaryResponse usageSummary = UsageSummaryResponse.builder()
                .userId(1L)
                .invoiceCount(1L)
                .apiCallCount(2L)
                .build();
        when(usageClient.getUsageSummary(1L)).thenReturn(usageSummary);

        com.saas.billing.model.Invoice mockInvoice = com.saas.billing.model.Invoice.builder()
                .id(999L)
                .userId(1L)
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .status(com.saas.billing.model.InvoiceStatus.GENERATED)
                .build();
        when(invoiceRepository.save(any(com.saas.billing.model.Invoice.class))).thenReturn(mockInvoice);

        InvoiceResponseDTO response = invoiceService.createInvoice(request, "raw_api_key");

        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertEquals("Customer Inc", response.getCustomerName());

        verify(usageClient, times(1)).createEvent(any());
        verify(invoiceRepository, times(1)).save(any());
    }

    @Test
    void testCreateInvoice_ExceedsLimit() {
        CreateInvoiceRequestDTO request = CreateInvoiceRequestDTO.builder()
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .build();

        when(apiKeyService.validateAndRetrieveKey("raw_api_key")).thenReturn(apiKey);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(subscription));

        // Mock usage exceeding limits (invoiceLimit = 3, currentCount = 3)
        UsageSummaryResponse usageSummary = UsageSummaryResponse.builder()
                .userId(1L)
                .invoiceCount(3L)
                .apiCallCount(2L)
                .build();
        when(usageClient.getUsageSummary(1L)).thenReturn(usageSummary);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            invoiceService.createInvoice(request, "raw_api_key");
        });

        assertTrue(exception.getMessage().contains("Invoice quota limit exceeded"));
        verify(invoiceRepository, never()).save(any());
        verify(usageClient, never()).createEvent(any());
    }
}
