package com.saas.billing.mapper;

import com.saas.billing.dto.response.SubscriptionResponse;
import com.saas.billing.entity.Subscription;

public class SubscriptionMapper {

    public static SubscriptionResponse toDTO(Subscription subscription) {
        return toDTO(subscription, null);
    }

    public static SubscriptionResponse toDTO(Subscription subscription, String apiKey) {
        if (subscription == null) return null;
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .planName(subscription.getPlan().getName())
                .planPrice(subscription.getPlan().getPrice())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .rawApiKey(apiKey)
                .build();
    }
}