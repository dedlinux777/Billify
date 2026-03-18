package com.billify.subscription;

import com.billify.dto.SubscriptionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe/{planId}")
    public ResponseEntity<SubscriptionDTO> subscribe(
            @AuthenticationPrincipal String email,
            @PathVariable Long planId) {
        return ResponseEntity.status(201)
                .body(subscriptionService.subscribe(email, planId));
    }

    @PutMapping("/cancel")
    public ResponseEntity<SubscriptionDTO> cancel(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(subscriptionService.cancel(email));
    }

    @PutMapping("/upgrade/{newPlanId}")
    public ResponseEntity<SubscriptionDTO> upgrade(
            @AuthenticationPrincipal String email,
            @PathVariable Long newPlanId) {
        return ResponseEntity.ok(subscriptionService.upgrade(email, newPlanId));
    }

    @GetMapping("/my")
    public ResponseEntity<SubscriptionDTO> getMySubscription(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(email));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionDTO>> getHistory(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(subscriptionService.getMyHistory(email));
    }
}