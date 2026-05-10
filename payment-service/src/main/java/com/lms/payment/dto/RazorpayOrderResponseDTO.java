package com.lms.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RazorpayOrderResponseDTO {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
    private String receipt;
}
