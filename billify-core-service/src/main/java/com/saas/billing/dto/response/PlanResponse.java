package com.saas.billing.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PlanResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer durationInDays;
    private Long invoiceLimit;
    private Long apiCallLimit;
}
