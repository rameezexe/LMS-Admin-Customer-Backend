package com.lms.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentReceiptRequestDTO {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
}
