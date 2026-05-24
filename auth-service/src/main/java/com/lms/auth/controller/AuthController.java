package com.lms.auth.controller;

import com.lms.auth.dto.*;
import com.lms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public auth endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserAccountResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        UserAccountResponseDTO response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout and revoke all refresh tokens", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Logout successful", null));
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password for current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        authService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    @PostMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Request password reset OTP via email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email", null));
    }

    @PostMapping("/reset-password")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC REGISTRATION FLOW (email-OTP → initiate → Razorpay → complete)
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/email/send-otp")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Send email-ownership OTP for new registrations")
    public ResponseEntity<ApiResponse<Void>> sendRegistrationOtp(@Valid @RequestBody EmailOtpRequestDTO request) {
        authService.sendRegistrationOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("OTP sent", null));
    }

    @PostMapping("/email/verify-otp")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Verify the registration OTP and obtain a short-lived verificationToken")
    public ResponseEntity<ApiResponse<EmailVerificationResponseDTO>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyEmailOtpRequestDTO request) {
        EmailVerificationResponseDTO response = authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Email verified", response));
    }

    @GetMapping("/username-available")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Check whether a username is free")
    public ResponseEntity<ApiResponse<UsernameAvailabilityResponseDTO>> isUsernameAvailable(
            @org.springframework.web.bind.annotation.RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.ok("ok", UsernameAvailabilityResponseDTO.builder().available(available).build()));
    }

    @PostMapping("/registration/initiate")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Validate signup data + create a Razorpay order. User account is NOT created yet.")
    public ResponseEntity<ApiResponse<RegistrationInitiateResponseDTO>> initiateRegistration(
            @Valid @RequestBody RegistrationInitiateRequestDTO request) {
        RegistrationInitiateResponseDTO response = authService.initiateRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration initiated", response));
    }

    @PostMapping("/registration/complete")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Verify the Razorpay payment and create the user account + member.")
    public ResponseEntity<ApiResponse<UserAccountResponseDTO>> completeRegistration(
            @Valid @RequestBody RegistrationCompleteRequestDTO request) {
        UserAccountResponseDTO response = authService.completeRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration complete", response));
    }
}
