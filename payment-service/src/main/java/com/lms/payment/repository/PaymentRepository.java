package com.lms.payment.repository;

import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMemberId(Long memberId);
    List<Payment> findByBorrowRecordId(Long borrowRecordId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findByMemberIdAndStatus(Long memberId, PaymentStatus status);
}
