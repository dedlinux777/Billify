package com.billify.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PlanDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer durationInDays;
}