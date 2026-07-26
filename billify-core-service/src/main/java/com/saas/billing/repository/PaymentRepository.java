package com.saas.billing.repository;

import com.saas.billing.entity.Payment;
import com.saas.billing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.subscription s JOIN FETCH s.plan WHERE s.user = :user")
    List<Payment> findAllByUser(@Param("user") User user);
}
