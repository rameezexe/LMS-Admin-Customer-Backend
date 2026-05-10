package com.lms.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinePolicyUpdateDTO {
    @NotNull(message = "Fine per day is required")
    private BigDecimal finePerDay;

    @NotNull(message = "Grace period days is required")
    private Integer gracePeriodDays;

    @NotNull(message = "Max fine amount is required")
    private BigDecimal maxFineAmount;
}
