package com.lms.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegistrationInitiateRequestDTO {
    @NotBlank
    private String verificationToken;

    @NotBlank @Email
    private String email;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may only contain letters, digits, dot, underscore, hyphen")
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Password must contain upper, lower, digit, and a symbol"
    )
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String membershipType;

    @NotBlank
    private String governmentIdUrl;

    @NotBlank
    private String membershipDuration;

    @NotNull
    @DecimalMin(value = "1.00", message = "Amount must be at least 1")
    private BigDecimal membershipAmount;
}
