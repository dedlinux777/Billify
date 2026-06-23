package com.saas.billing.subscription;

import com.saas.billing.dto.SubscriptionDTO;
import com.saas.billing.exception.InvalidSubscriptionException;
import com.saas.billing.exception.ResourceNotFoundException;
import com.saas.billing.mapper.SubscriptionMapper;
import com.saas.billing.model.Plan;
import com.saas.billing.model.Subscription;
import com.saas.billing.model.SubscriptionStatus;
import com.saas.billing.model.User;
import com.saas.billing.payment.PaymentService;
import com.saas.billing.repository.PlanRepository;
import com.saas.billing.repository.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    // ── SUBSCRIBE service: subscribe to a plan
    @Transactional
    public SubscriptionDTO subscribe(String email, Long planId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Enforce one active subscription rule
        boolean hasActive = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .isPresent();

        if (hasActive) {
            throw new InvalidSubscriptionException(
                    "You already have an active subscription. Cancel it before subscribing to a new plan.");
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found with id: " + planId));

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationInDays()))
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        paymentService.processPayment(saved);
        log.info("User {} subscribed to plan {}", email, plan.getName());
        return SubscriptionMapper.toDTO(saved);
    }

    // ── CANCEL ─────────────────────────────────────────────
    @Transactional
    public SubscriptionDTO cancel(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException("No active subscription found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);

        log.info("User {} cancelled subscription to plan {}",
                email, subscription.getPlan().getName());
        return SubscriptionMapper.toDTO(saved);
    }

    // ── UPGRADE ────────────────────────────────────────────
    @Transactional
    public SubscriptionDTO upgrade(String email, Long newPlanId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription current = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException(
                                "No active subscription to upgrade"));

        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plan not found with id: " + newPlanId));

        if (current.getPlan().getId().equals(newPlanId)) {
            throw new InvalidSubscriptionException(
                    "You are already on this plan");
        }

        // Cancel current
        current.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(current);

        // Create new
        LocalDateTime now = LocalDateTime.now();
        Subscription upgraded = Subscription.builder()
                .user(user)
                .plan(newPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(newPlan.getDurationInDays()))
                .build();

        Subscription saved = subscriptionRepository.save(upgraded);

        // Process payment for new plan — rolls back if failed
        paymentService.processPayment(saved);
        log.info("User {} upgraded from plan {} to plan {}",
                email, current.getPlan().getName(), newPlan.getName());
        return SubscriptionMapper.toDTO(saved);

    }

    // ── VIEW MY SUBSCRIPTION ───────────────────────────────
    public SubscriptionDTO getMySubscription(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException("No active subscription found"));

        return SubscriptionMapper.toDTO(subscription);
    }

    // ── VIEW ALL MY SUBSCRIPTIONS (history) ───────────────
    public List<SubscriptionDTO> getMyHistory(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return subscriptionRepository.findAllByUser(user)
                .stream()
                .map(SubscriptionMapper::toDTO)
                .toList();
    }
}