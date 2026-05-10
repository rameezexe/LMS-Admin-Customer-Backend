package com.lms.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RazorpayOrderRequestDTO {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Borrow Record ID is required")
    private Long borrowRecordId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
