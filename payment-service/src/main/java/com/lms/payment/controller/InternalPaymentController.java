package com.lms.payment.controller;

import com.lms.payment.dto.RazorpayOrderResponseDTO;
import com.lms.payment.dto.RefundResponseDTO;
import com.lms.payment.entity.Payment;
import com.lms.payment.service.RazorpayService;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/payments")
@Tag(name = "Internal - Payments", description = "Internal endpoints for microservice communication")
public class InternalPaymentController {

    private final RazorpayService razorpayService;

    public InternalPaymentController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @PostMapping("/membership/create-order")
    @Operation(summary = "Create Razorpay order for membership payment")
    public ResponseEntity<Map<String, Object>> createMembershipOrder(@RequestBody Map<String, Object> request) throws RazorpayException {
        // memberId is nullable: pre-pay registration creates the order BEFORE
        // the member row exists. Auth-service later calls /attach-member.
        Object rawMemberId = request.get("memberId");
        Long memberId = (rawMemberId instanceof Number) ? ((Number) rawMemberId).longValue() : null;

        if (request.get("amount") == null) {
            throw new IllegalArgumentException("amount is required");
        }
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        RazorpayOrderResponseDTO order = razorpayService.createMembershipOrder(memberId, amount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "orderId", order.getOrderId(),
                        "amount", order.getAmount(),
                        "currency", order.getCurrency(),
                        "keyId", order.getKeyId()
                )
        ));
    }

    @PostMapping("/membership/verify")
    @Operation(summary = "Verify Razorpay membership payment")
    public ResponseEntity<Map<String, Object>> verifyMembershipPayment(@RequestBody Map<String, String> request) {
        String orderId = request.get("razorpay_order_id");
        String paymentId = request.get("razorpay_payment_id");
        String signature = request.get("razorpay_signature");

        Payment payment = razorpayService.verifyPayment(orderId, paymentId, signature);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "paymentId", payment.getTransactionId(),
                        "status", payment.getStatus().name()
                )
        ));
    }

    @PostMapping("/membership/attach-member")
    @Operation(summary = "Attach a memberId to a pending membership Payment (after pre-pay registration completes)")
    public ResponseEntity<Map<String, Object>> attachMember(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("razorpayOrderId");
        Long memberId = ((Number) request.get("memberId")).longValue();
        razorpayService.attachMemberId(orderId, memberId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment by Razorpay payment ID (internal)")
    public ResponseEntity<Map<String, Object>> refundPayment(@RequestBody Map<String, String> request) throws RazorpayException {
        String razorpayPaymentId = request.get("razorpayPaymentId");

        RefundResponseDTO refund = razorpayService.refundByRazorpayPaymentId(razorpayPaymentId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "refundId", refund.getRefundId(),
                        "status", refund.getStatus(),
                        "amount", refund.getAmount()
                )
        ));
    }
}
