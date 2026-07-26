package com.saas.billing.mapper;

import com.saas.billing.dto.response.PaymentResponse;
import com.saas.billing.entity.Payment;

public class PaymentMapper {

    public static PaymentResponse toDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentResponse.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .planName(payment.getSubscription().getPlan().getName())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}