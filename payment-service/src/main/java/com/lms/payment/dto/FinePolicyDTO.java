package com.lms.payment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FinePolicyDTO {
    private Long id;
    private BigDecimal finePerDay;
    private int gracePeriodDays;
    private BigDecimal maxFineAmount;
    private LocalDate effectiveDate;
}
