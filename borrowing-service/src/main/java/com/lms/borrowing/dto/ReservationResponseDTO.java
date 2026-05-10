package com.lms.borrowing.dto;

import com.lms.borrowing.entity.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReservationResponseDTO {
    private Long id;
    private Long memberId;
    private Long bookId;
    private LocalDate reservationDate;
    private LocalDate expiryDate;
    private ReservationStatus status;
}
