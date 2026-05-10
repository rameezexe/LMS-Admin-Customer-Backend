package com.lms.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenueDTO {
    private Integer month;
    private Integer year;
    private BigDecimal totalRevenue;
    private long paymentCount;
}
