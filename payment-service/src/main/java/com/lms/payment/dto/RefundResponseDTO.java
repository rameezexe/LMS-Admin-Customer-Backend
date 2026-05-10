package com.lms.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundResponseDTO {
    private String refundId;
    private String status;
    private BigDecimal amount;
}
