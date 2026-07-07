package com.saas.usage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "usage_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
