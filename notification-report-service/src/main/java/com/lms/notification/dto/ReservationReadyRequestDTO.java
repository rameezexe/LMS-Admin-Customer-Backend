package com.lms.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationReadyRequestDTO {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotBlank(message = "Book title is required")
    private String bookTitle;

    private Integer pickupWindowDays;
}
