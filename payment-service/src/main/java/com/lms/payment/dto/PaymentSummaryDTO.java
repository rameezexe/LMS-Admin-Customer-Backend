package com.lms.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSummaryDTO {
    private BigDecimal totalPaid;
    private BigDecimal totalRefunded;
    private long paymentCount;
}
