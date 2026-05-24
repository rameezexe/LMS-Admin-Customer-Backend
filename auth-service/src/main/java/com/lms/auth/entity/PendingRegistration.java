package com.lms.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Holds a partially-validated registration in flight between Razorpay order
 * creation and payment verification. Created by registration/initiate,
 * consumed by registration/complete. Expires after 30 minutes so that
 * abandoned signups don't accumulate.
 */
@Entity
@Table(name = "pending_registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String pendingId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String membershipType;

    private String governmentIdUrl;

    @Column(length = 20)
    private String membershipDuration;

    @Column(precision = 10, scale = 2)
    private BigDecimal membershipAmount;

    private String razorpayOrderId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
