package com.saas.execution.invoice;

import com.saas.execution.client.ManagementClient;
import com.saas.execution.client.UsageClient;
import com.saas.execution.client.dto.ApiKeyValidationResponse;
import com.saas.execution.client.dto.SubscriptionQuotaResponse;
import com.saas.execution.client.dto.UsageSummaryResponse;
import com.saas.execution.dto.request.CreateInvoiceRequest;
import com.saas.execution.dto.response.InvoiceResponse;
import com.saas.execution.entity.Invoice;
import com.saas.execution.entity.InvoiceStatus;
import com.saas.execution.service.InvoicePersistenceService;
import com.saas.execution.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private ManagementClient managementClient;

    @Mock
    private UsageClient usageClient;

    @Mock
    private InvoicePersistenceService invoicePersistenceService;

    @InjectMocks
    private InvoiceService invoiceService;

    private CreateInvoiceRequest request;

    @BeforeEach
    void setUp() {
        request = CreateInvoiceRequest.builder()
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .build();
    }

    @Test
    void testCreateInvoice_Success() {
        ApiKeyValidationResponse keyResponse = ApiKeyValidationResponse.builder()
                .userId(1L)
                .apiKeyId(100L)
                .active(true)
                .build();

        SubscriptionQuotaResponse quotaResponse = SubscriptionQuotaResponse.builder()
                .subscriptionId(50L)
                .planName("Pro Plan")
                .invoiceLimit(3L)
                .apiCallLimit(10L)
                .active(true)
                .build();

        UsageSummaryResponse usageResponse = UsageSummaryResponse.builder()
                .userId(1L)
                .invoiceCount(1L)
                .apiCallCount(2L)
                .build();

        Invoice mockInvoice = Invoice.builder()
                .id(999L)
                .userId(1L)
                .customerName("Customer Inc")
                .amount(BigDecimal.valueOf(150.00))
                .status(InvoiceStatus.GENERATED)
                .build();

        when(managementClient.validateKey("raw_api_key")).thenReturn(keyResponse);
        when(managementClient.getActiveSubscriptionQuota(1L)).thenReturn(quotaResponse);
        when(usageClient.getUsageSummary(1L)).thenReturn(usageResponse);
        when(invoicePersistenceService.saveInvoice(any(Long.class), any(CreateInvoiceRequest.class)))
                .thenReturn(mockInvoice);

        InvoiceResponse response = invoiceService.createInvoice(request, "raw_api_key");

        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertEquals("Customer Inc", response.getCustomerName());

        verify(invoicePersistenceService, times(1)).saveInvoice(any(), any());
        verify(usageClient, times(1)).createEvent(any());
    }

    @Test
    void testCreateInvoice_ExceedsLimit() {
        ApiKeyValidationResponse keyResponse = ApiKeyValidationResponse.builder()
                .userId(1L)
                .apiKeyId(100L)
                .active(true)
                .build();

        SubscriptionQuotaResponse quotaResponse = SubscriptionQuotaResponse.builder()
                .subscriptionId(50L)
                .planName("Pro Plan")
                .invoiceLimit(3L)
                .apiCallLimit(10L)
                .active(true)
                .build();

        UsageSummaryResponse usageResponse = UsageSummaryResponse.builder()
                .userId(1L)
                .invoiceCount(3L) // Limit is 3, current is 3
                .apiCallCount(2L)
                .build();

        when(managementClient.validateKey("raw_api_key")).thenReturn(keyResponse);
        when(managementClient.getActiveSubscriptionQuota(1L)).thenReturn(quotaResponse);
        when(usageClient.getUsageSummary(1L)).thenReturn(usageResponse);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            invoiceService.createInvoice(request, "raw_api_key");
        });

        assertTrue(exception.getMessage().contains("Invoice quota limit exceeded"));
        verify(invoicePersistenceService, never()).saveInvoice(any(), any());
        verify(usageClient, never()).createEvent(any());
    }
}
