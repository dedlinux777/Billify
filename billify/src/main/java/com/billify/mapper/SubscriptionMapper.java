package com.billify.mapper;

import com.billify.dto.SubscriptionDTO;
import com.billify.model.Subscription;

public class SubscriptionMapper {

    public static SubscriptionDTO toDTO(Subscription subscription) {
        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .planName(subscription.getPlan().getName())
                .planPrice(subscription.getPlan().getPrice())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .build();
    }
}