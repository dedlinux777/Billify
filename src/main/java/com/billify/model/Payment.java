package com.billify.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Double amount;

    private LocalDateTime paymentDate;

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", subscription=" + subscription +
                ", status=" + status +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                '}';
    }
}