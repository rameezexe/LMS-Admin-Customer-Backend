package com.lms.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String address;

    @Column(nullable = false, unique = true)
    private String membershipNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipType membershipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(nullable = false)
    private LocalDate joinDate;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // ── Registration Approval Fields ──

    @Column(name = "government_id_url", length = 500)
    private String governmentIdUrl;

    @Column(name = "membership_duration", length = 20)
    private String membershipDuration; // 3_MONTHS, 6_MONTHS, 1_YEAR

    @Column(name = "membership_amount", precision = 10, scale = 2)
    private BigDecimal membershipAmount;

    @Column(name = "membership_payment_id", length = 100)
    private String membershipPaymentId; // Razorpay payment ID for refund

    @Column(name = "membership_expiry_date")
    private LocalDate membershipExpiryDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (profilePhotoUrl == null) profilePhotoUrl = "";
        if (phone == null) phone = "";
        if (address == null) address = "";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
