package com.lms.member.dto;

import com.lms.member.entity.MembershipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MemberRegistrationDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;
    private MembershipType membershipType;

    // ── Registration fields ──
    private String governmentIdUrl;
    private String membershipDuration;  // 3_MONTHS, 6_MONTHS, 1_YEAR
    private BigDecimal membershipAmount;
    private String membershipPaymentId;
}
