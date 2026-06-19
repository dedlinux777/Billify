package com.billify.mapper;

import com.billify.dto.PlanDTO;
import com.billify.model.Plan;

public class PlanMapper {

    public static PlanDTO toDTO(Plan plan) {
        return PlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationInDays(plan.getDurationInDays())
                .build();
    }

    public static Plan toEntity(PlanDTO dto) {
        return Plan.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .durationInDays(dto.getDurationInDays())
                .build();
    }

//    Why a toEntity here but not in UserMapper? Because admins create plans via API — you need to convert incoming DTO → entity. Users are only created via registration which has its own RegisterRequest.
}