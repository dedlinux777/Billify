package com.saas.billing.mapper;

import com.saas.billing.dto.PaymentDTO;
import com.saas.billing.model.Payment;

public class PaymentMapper {

    public static PaymentDTO toDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .planName(payment.getSubscription().getPlan().getName())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}