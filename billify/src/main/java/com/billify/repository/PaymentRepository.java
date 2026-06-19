package com.billify.repository;

import com.billify.model.Payment;
import com.billify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.subscription.user = :user")
    List<Payment> findAllByUser(User user);
}