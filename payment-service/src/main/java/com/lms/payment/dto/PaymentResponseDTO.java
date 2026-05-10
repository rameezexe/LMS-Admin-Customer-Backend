package com.lms.payment.dto;

import com.lms.payment.entity.PaymentMethod;
import com.lms.payment.entity.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponseDTO {
    private Long id;
    private Long memberId;
    private Long borrowRecordId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String razorpayOrderId;
    private String notes;
}
