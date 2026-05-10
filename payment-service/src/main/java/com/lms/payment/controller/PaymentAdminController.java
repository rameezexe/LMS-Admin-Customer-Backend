package com.lms.payment.controller;

import com.lms.payment.dto.ApiResponse;
import com.lms.payment.dto.PaymentResponseDTO;
import com.lms.payment.dto.PaymentSummaryDTO;
import com.lms.payment.dto.RefundResponseDTO;
import com.lms.payment.dto.RevenueDTO;
import com.lms.payment.service.PaymentService;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Payments", description = "Admin endpoints for managing payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentAdminController {

    private final PaymentService paymentService;

    public PaymentAdminController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Get all payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponseDTO>>> getAllPayments(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved successfully", paymentService.getAllPayments(pageable)));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending payments")
    public ResponseEntity<ApiResponse<List<PaymentResponseDTO>>> getPendingPayments() {
        return ResponseEntity.ok(ApiResponse.ok("Pending payments retrieved", paymentService.getPendingPayments()));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment")
    public ResponseEntity<ApiResponse<RefundResponseDTO>> refundPayment(@PathVariable Long id) throws RazorpayException {
        return ResponseEntity.ok(ApiResponse.ok("Refund processed successfully", paymentService.refundPayment(id)));
    }

    @GetMapping("/member/{memberId}/summary")
    @Operation(summary = "Get payment summary for a member")
    public ResponseEntity<ApiResponse<PaymentSummaryDTO>> getMemberPaymentSummary(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("Summary retrieved", paymentService.getMemberPaymentSummary(memberId)));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Calculate revenue")
    public ResponseEntity<ApiResponse<RevenueDTO>> calculateRevenue(@RequestParam(required = false) Integer month,
                                                                    @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok("Revenue calculated", paymentService.calculateRevenue(month, year)));
    }
}
