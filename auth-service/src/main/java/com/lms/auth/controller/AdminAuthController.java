package com.lms.auth.controller;

import com.lms.auth.dto.ApiResponse;
import com.lms.auth.dto.UserAccountResponseDTO;
import com.lms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Auth Management", description = "Admin-only user account management")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all user accounts")
    public ResponseEntity<ApiResponse<List<UserAccountResponseDTO>>> getAllUsers() {
        List<UserAccountResponseDTO> users = authService.getAllUserAccounts();
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", users));
    }

    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@PathVariable Long id) {
        authService.deactivateAccount(id);
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated successfully", null));
    }

    @PutMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@PathVariable Long id) {
        authService.activateAccount(id);
        return ResponseEntity.ok(ApiResponse.ok("Account activated successfully", null));
    }
}
