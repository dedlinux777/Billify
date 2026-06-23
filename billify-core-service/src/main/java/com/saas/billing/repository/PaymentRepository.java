package com.saas.billing.repository;

import com.saas.billing.model.Payment;
import com.saas.billing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.subscription.user = :user")
    List<Payment> findAllByUser(User user);
}