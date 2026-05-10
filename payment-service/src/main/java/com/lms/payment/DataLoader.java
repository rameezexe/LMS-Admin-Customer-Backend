package com.lms.payment;

import com.lms.payment.entity.FinePolicy;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.PaymentMethod;
import com.lms.payment.entity.PaymentStatus;
import com.lms.payment.repository.FinePolicyRepository;
import com.lms.payment.repository.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class DataLoader implements CommandLineRunner {

    private final FinePolicyRepository finePolicyRepository;
    private final PaymentRepository paymentRepository;

    public DataLoader(FinePolicyRepository finePolicyRepository, PaymentRepository paymentRepository) {
        this.finePolicyRepository = finePolicyRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(String... args) {
        if (finePolicyRepository.count() == 0) {
            FinePolicy policy = FinePolicy.builder()
                    .finePerDay(new BigDecimal("5.00"))
                    .gracePeriodDays(0)
                    .maxFineAmount(new BigDecimal("500.00"))
                    .effectiveDate(LocalDate.now().minusDays(30))
                    .build();
            finePolicyRepository.save(policy);
            System.out.println("=== Payment DataLoader: Seeded FinePolicy ===");
        }

        if (paymentRepository.count() == 0) {
            Payment p1 = Payment.builder()
                    .memberId(1L)
                    .borrowRecordId(1L)
                    .amount(new BigDecimal("50.00"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.CARD)
                    .status(PaymentStatus.COMPLETED)
                    .transactionId(UUID.randomUUID().toString())
                    .notes("Initial card payment")
                    .build();
            paymentRepository.save(p1);

            Payment p2 = Payment.builder()
                    .memberId(1L)
                    .borrowRecordId(2L)
                    .amount(new BigDecimal("25.00"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.UPI)
                    .status(PaymentStatus.COMPLETED)
                    .transactionId(UUID.randomUUID().toString())
                    .notes("UPI payment")
                    .build();
            paymentRepository.save(p2);

            Payment p3 = Payment.builder()
                    .memberId(2L)
                    .borrowRecordId(3L)
                    .amount(new BigDecimal("75.00"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .status(PaymentStatus.COMPLETED)
                    .transactionId("pay_Mock12345")
                    .razorpayOrderId("order_Mock12345")
                    .razorpaySignature("mock_signature")
                    .notes("Mock Razorpay payment")
                    .build();
            paymentRepository.save(p3);

            System.out.println("=== Payment DataLoader: Seeded Payments ===");
        }
    }
}
