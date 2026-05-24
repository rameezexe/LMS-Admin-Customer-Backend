package com.lms.payment.service;

import com.lms.payment.dto.PaymentResponseDTO;
import com.lms.payment.dto.PaymentSummaryDTO;
import com.lms.payment.dto.RefundResponseDTO;
import com.lms.payment.dto.RevenueDTO;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentMethod;
import com.lms.payment.entity.PaymentStatus;
import com.lms.payment.repository.PaymentRepository;
import com.lms.payment.security.SecurityHelper;
import com.razorpay.RazorpayException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final SecurityHelper securityHelper;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public PaymentService(PaymentRepository paymentRepository, RazorpayService razorpayService, SecurityHelper securityHelper, org.springframework.web.client.RestTemplate restTemplate) {
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.securityHelper = securityHelper;
        this.restTemplate = restTemplate;
    }

    private PaymentResponseDTO mapToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .memberId(payment.getMemberId())
                .borrowRecordId(payment.getBorrowRecordId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .notes(payment.getNotes())
                .build();
    }

    private void verifyOwnership(Long memberId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        if (!securityHelper.isAdmin() && (tokenMemberId == null || !tokenMemberId.equals(memberId))) {
            throw new AccessDeniedException("You can only access your own payments.");
        }
    }

    @Transactional
    public PaymentResponseDTO processPayment(Long memberId, Long borrowRecordId, BigDecimal amount, PaymentMethod method, String notes) {
        verifyOwnership(memberId);

        if (method == PaymentMethod.RAZORPAY) {
            throw new IllegalArgumentException("Use /create-order endpoint for Razorpay payments");
        }

        Payment payment = Payment.builder()
                .memberId(memberId)
                .borrowRecordId(borrowRecordId)
                .amount(amount)
                .paymentDate(LocalDate.now())
                .paymentMethod(method)
                .status(PaymentStatus.COMPLETED)
                .transactionId(UUID.randomUUID().toString())
                .notes(notes)
                .build();

        Payment saved = paymentRepository.save(payment);
        sendPaymentNotification(saved.getMemberId(), saved.getAmount(), saved.getTransactionId());
        return mapToDTO(saved);
    }

    public void sendPaymentNotification(Long memberId, BigDecimal amount, String transactionId) {
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

    public List<PaymentResponseDTO> getPaymentsByMember(Long memberId) {
        verifyOwnership(memberId);
        return paymentRepository.findByMemberId(memberId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PaymentResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + id));
        verifyOwnership(payment.getMemberId());
        return mapToDTO(payment);
    }

    public Page<PaymentResponseDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<PaymentResponseDTO> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RefundResponseDTO refundPayment(Long paymentId) throws RazorpayException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only completed or pending payments can be refunded");
        }

        // For Razorpay payments with a valid transaction ID, attempt API refund.
        // Surface failures — don't silently mark as refunded locally,
        // which would let admin see "success" while the customer is never refunded.
        if (payment.getPaymentMethod() == PaymentMethod.RAZORPAY
                && payment.getTransactionId() != null
                && !payment.getTransactionId().isBlank()) {
            return razorpayService.refundRazorpayPayment(paymentId);
        }

        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            throw new IllegalArgumentException("Cash payments are non-refundable");
        }

        // For non-Razorpay or Razorpay-pending payments, refund locally
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return RefundResponseDTO.builder()
                .refundId(UUID.randomUUID().toString())
                .status("processed")
                .amount(payment.getAmount())
                .build();
    }

    public PaymentSummaryDTO getMemberPaymentSummary(Long memberId) {
        verifyOwnership(memberId);
        List<Payment> payments = paymentRepository.findByMemberId(memberId);

        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunded = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentSummaryDTO.builder()
                .totalPaid(totalPaid)
                .totalRefunded(totalRefunded)
                .paymentCount(payments.size())
                .build();
    }

    public RevenueDTO calculateRevenue(Integer month, Integer year) {
        List<Payment> completedPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);

        if (month != null && year != null) {
            completedPayments = completedPayments.stream()
                    .filter(p -> p.getPaymentDate().getMonthValue() == month && p.getPaymentDate().getYear() == year)
                    .collect(Collectors.toList());
        } else if (year != null) {
            completedPayments = completedPayments.stream()
                    .filter(p -> p.getPaymentDate().getYear() == year)
                    .collect(Collectors.toList());
        }

        BigDecimal totalRevenue = completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueDTO.builder()
                .month(month)
                .year(year)
                .totalRevenue(totalRevenue)
                .paymentCount(completedPayments.size())
                .build();
    }
}
