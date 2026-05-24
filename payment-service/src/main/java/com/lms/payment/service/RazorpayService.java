package com.lms.payment.service;

import com.lms.payment.dto.RazorpayOrderResponseDTO;
import com.lms.payment.dto.RefundResponseDTO;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentMethod;
import com.lms.payment.entity.PaymentStatus;
import com.lms.payment.exception.PaymentVerificationException;
import com.lms.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public RazorpayService(RazorpayClient razorpayClient, PaymentRepository paymentRepository, org.springframework.web.client.RestTemplate restTemplate) {
        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public RazorpayOrderResponseDTO createOrder(Long memberId, Long borrowRecordId, BigDecimal amount) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + borrowRecordId + "_" + UUID.randomUUID().toString().substring(0, 8));

        Order order = razorpayClient.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .memberId(memberId)
                .borrowRecordId(borrowRecordId)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.RAZORPAY)
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(order.get("id"))
                .build();

        paymentRepository.save(payment);

        return RazorpayOrderResponseDTO.builder()
                .orderId(order.get("id"))
                .amount(amount)
                .currency("INR")
                .keyId(keyId)
                .receipt(order.get("receipt"))
                .build();
    }

    @Transactional
    public Payment verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with Razorpay Order ID: " + razorpayOrderId));

        try {
            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            if (isValid) {
                payment.setTransactionId(razorpayPaymentId);
                payment.setRazorpaySignature(razorpaySignature);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDate.now());
                Payment saved = paymentRepository.save(payment);
                sendPaymentNotification(saved.getMemberId(), saved.getAmount(), saved.getTransactionId());
                return saved;
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentVerificationException("Payment signature verification failed");
            }
        } catch (RazorpayException e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentVerificationException("Error verifying payment signature: " + e.getMessage());
        }
    }

    @Transactional
    public RefundResponseDTO refundRazorpayPayment(Long paymentId) throws RazorpayException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new IllegalArgumentException("Payment is not a Razorpay payment");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed payments can be refunded");
        }

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", payment.getAmount().multiply(new BigDecimal(100)).intValue());

        Refund refund = razorpayClient.payments.refund(payment.getTransactionId(), refundRequest);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return RefundResponseDTO.builder()
                .refundId(refund.get("id"))
                .status(refund.get("status"))
                .amount(payment.getAmount())
                .build();
    }

    private void sendPaymentNotification(Long memberId, BigDecimal amount, String transactionId) {
        if (memberId == null) {
            // Membership payments during pre-pay registration don't have a
            // memberId yet — auth-service will trigger the receipt later.
            return;
        }
        try {
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("memberId", memberId);
            request.put("amount", amount);
            request.put("transactionId", transactionId);

            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/payment",
                    request,
                    Object.class
            );
        } catch (Exception e) {
            System.err.println("Failed to send payment notification: " + e.getMessage());
        }
    }

    // ── Membership Payment Methods ──────────────────────────────────────

    @Transactional
    public RazorpayOrderResponseDTO createMembershipOrder(Long memberId, BigDecimal amount) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue());
        orderRequest.put("currency", "INR");
        String receiptOwner = memberId != null ? memberId.toString() : "pending";
        orderRequest.put("receipt", "membership_" + receiptOwner + "_" + UUID.randomUUID().toString().substring(0, 8));

        Order order = razorpayClient.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .memberId(memberId)
                .borrowRecordId(null)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.RAZORPAY)
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(order.get("id"))
                .purpose("MEMBERSHIP")
                .build();

        paymentRepository.save(payment);

        return RazorpayOrderResponseDTO.builder()
                .orderId(order.get("id"))
                .amount(amount)
                .currency("INR")
                .keyId(keyId)
                .receipt(order.get("receipt"))
                .build();
    }

    /**
     * Attach a memberId to an existing PENDING/COMPLETED membership Payment.
     * Used by auth-service when a pre-pay registration completes and we
     * finally know the real memberId.
     */
    @Transactional
    public void attachMemberId(String razorpayOrderId, Long memberId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with order id: " + razorpayOrderId));
        payment.setMemberId(memberId);
        paymentRepository.save(payment);
    }

    @Transactional
    public RefundResponseDTO refundByRazorpayPaymentId(String razorpayPaymentId) throws RazorpayException {
        Payment payment = paymentRepository.findByTransactionId(razorpayPaymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with Razorpay Payment ID: " + razorpayPaymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed payments can be refunded");
        }

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", payment.getAmount().multiply(new BigDecimal(100)).intValue());

        Refund refund = razorpayClient.payments.refund(payment.getTransactionId(), refundRequest);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return RefundResponseDTO.builder()
                .refundId(refund.get("id"))
                .status(refund.get("status"))
                .amount(payment.getAmount())
                .build();
    }
}
