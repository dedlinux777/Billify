package com.saas.billing.repository;

import com.saas.billing.model.Subscription;
import com.saas.billing.model.SubscriptionStatus;
import com.saas.billing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    List<Subscription> findAllByUser(User user);
}