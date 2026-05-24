package com.lms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Legacy registration DTO (admin path still uses /api/auth/register).
 * Public user signup now goes through registration/initiate +
 * registration/complete and uses {@link RegistrationInitiateRequestDTO}.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequestDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, dot, underscore, hyphen")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Password must contain upper, lower, digit, and a symbol"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String membershipType;
    private String governmentIdUrl;
    private String membershipDuration;
    private BigDecimal membershipAmount;
    private String membershipPaymentId;

    private String role;
    private String adminSecret;
    private Long memberId;
}
