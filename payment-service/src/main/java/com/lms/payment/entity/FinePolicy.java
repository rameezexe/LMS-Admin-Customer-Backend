package com.lms.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fine_policies")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FinePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(nullable = false)
    private int gracePeriodDays;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxFineAmount;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
