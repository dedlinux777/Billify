package com.saas.billing.invoice;

import com.saas.billing.client.UsageClient;
import com.saas.billing.client.dto.UsageSummaryResponse;
import com.saas.billing.dto.request.CreateInvoiceRequest;
import com.saas.billing.dto.response.InvoiceResponse;
import com.saas.billing.entity.*;
import com.saas.billing.repository.*;
import com.saas.billing.service.*;
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
    private UsageClient usageClient;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InvoicePersistenceService invoicePersistenceService;

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
                .user(user)
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
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
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

        Invoice mockInvoice = Invoice.builder()
                .id(999L)
                .user(user)
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .status(InvoiceStatus.GENERATED)
                .build();
        when(invoicePersistenceService.saveInvoiceAndPropagate(any(User.class), any(ApiKey.class), any(CreateInvoiceRequest.class)))
                .thenReturn(mockInvoice);

        InvoiceResponse response = invoiceService.createInvoice(request, "raw_api_key");

        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertEquals("Customer Inc", response.getCustomerName());

        verify(invoicePersistenceService, times(1)).saveInvoiceAndPropagate(any(), any(), any());
    }

    @Test
    void testCreateInvoice_ExceedsLimit() {
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
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
        verify(invoicePersistenceService, never()).saveInvoiceAndPropagate(any(), any(), any());
    }
}
