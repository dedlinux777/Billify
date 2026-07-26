package com.saas.billing.mapper;

import com.saas.billing.dto.request.PlanRequest;
import com.saas.billing.dto.response.PlanResponse;
import com.saas.billing.entity.Plan;

public class PlanMapper {

    public static PlanResponse toDTO(Plan plan) {
        if (plan == null) return null;
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationInDays(plan.getDurationInDays())
                .invoiceLimit(plan.getInvoiceLimit())
                .apiCallLimit(plan.getApiCallLimit())
                .build();
    }

    public static Plan toEntity(PlanRequest request) {
        if (request == null) return null;
        return Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationInDays(request.getDurationInDays())
                .invoiceLimit(request.getInvoiceLimit())
                .apiCallLimit(request.getApiCallLimit())
                .build();
    }
}