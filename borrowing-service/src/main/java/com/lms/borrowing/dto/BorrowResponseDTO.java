package com.lms.borrowing.dto;

import com.lms.borrowing.entity.BorrowStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BorrowResponseDTO {
    private Long id;
    private Long memberId;
    private Long bookCopyId;
    private Long bookId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BorrowStatus status;
    private BigDecimal fineAmount;
    private boolean finePaid;
    private Long issuedByLibrarianId;
}
