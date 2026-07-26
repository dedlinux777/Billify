package com.saas.billing.controller;

import com.saas.billing.dto.response.SubscriptionQuotaResponse;
import com.saas.billing.entity.SubscriptionStatus;
import com.saas.billing.entity.User;
import com.saas.billing.repository.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import com.saas.billing.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class InternalSubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @GetMapping("/active-quota")
    public ResponseEntity<SubscriptionQuotaResponse> getActiveSubscriptionQuota(@RequestParam("userId") Long userId) {
        log.info("Internal request to fetch active subscription quota for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        SubscriptionQuotaResponse response = subscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .map(sub -> SubscriptionQuotaResponse.builder()
                        .subscriptionId(sub.getId())
                        .planName(sub.getPlan().getName())
                        .invoiceLimit(sub.getPlan().getInvoiceLimit())
                        .apiCallLimit(sub.getPlan().getApiCallLimit())
                        .active(true)
                        .build())
                .orElse(SubscriptionQuotaResponse.builder()
                        .active(false)
                        .build());
        return ResponseEntity.ok(response);
    }
}
