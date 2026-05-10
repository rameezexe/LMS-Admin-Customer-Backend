package com.lms.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OverdueAlertRequestDTO {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotBlank(message = "Book title is required")
    private String bookTitle;

    @NotNull(message = "Fine amount is required")
    private BigDecimal fineAmount;
}
