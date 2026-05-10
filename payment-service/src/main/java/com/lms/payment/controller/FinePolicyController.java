package com.lms.payment.controller;

import com.lms.payment.dto.ApiResponse;
import com.lms.payment.dto.FinePolicyDTO;
import com.lms.payment.dto.FinePolicyUpdateDTO;
import com.lms.payment.service.FinePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments/policy")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Fine Policy", description = "Admin endpoints for fine policy")
@SecurityRequirement(name = "bearerAuth")
public class FinePolicyController {

    private final FinePolicyService finePolicyService;

    public FinePolicyController(FinePolicyService finePolicyService) {
        this.finePolicyService = finePolicyService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current fine policy")
    public ResponseEntity<ApiResponse<FinePolicyDTO>> getCurrentPolicy() {
        return ResponseEntity.ok(ApiResponse.ok("Current policy retrieved", finePolicyService.getCurrentPolicy()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get fine policy history")
    public ResponseEntity<ApiResponse<List<FinePolicyDTO>>> getPolicyHistory() {
        return ResponseEntity.ok(ApiResponse.ok("Policy history retrieved", finePolicyService.getPolicyHistory()));
    }

    @PostMapping("/update")
    @Operation(summary = "Update fine policy")
    public ResponseEntity<ApiResponse<FinePolicyDTO>> updatePolicy(@Valid @RequestBody FinePolicyUpdateDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Policy updated successfully", finePolicyService.updatePolicy(request)));
    }
}
