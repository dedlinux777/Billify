package com.saas.usage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usage_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "invoice_count", nullable = false)
    private Long invoiceCount;

    @Column(name = "api_call_count", nullable = false)
    private Long apiCallCount;
}
