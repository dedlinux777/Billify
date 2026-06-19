package com.billify.mapper;

import com.billify.dto.PaymentDTO;
import com.billify.model.Payment;

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