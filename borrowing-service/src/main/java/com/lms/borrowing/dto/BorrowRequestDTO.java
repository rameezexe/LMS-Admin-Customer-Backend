package com.lms.borrowing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequestDTO {
    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Book Copy ID is required")
    private Long bookCopyId;

    @NotNull(message = "Book ID is required")
    private Long bookId;

    private Long librarianId;
}
