package com.saas.billing.repository;

import com.saas.billing.entity.Subscription;
import com.saas.billing.entity.SubscriptionStatus;
import com.saas.billing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan WHERE s.user = :user AND s.status = :status")
    Optional<Subscription> findByUserAndStatus(@Param("user") User user, @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan WHERE s.user = :user")
    List<Subscription> findAllByUser(@Param("user") User user);
}
