package com.billify.repository;

import com.billify.model.Subscription;
import com.billify.model.SubscriptionStatus;
import com.billify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    List<Subscription> findAllByUser(User user);
}