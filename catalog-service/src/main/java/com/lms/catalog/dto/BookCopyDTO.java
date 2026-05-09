package com.lms.catalog.dto;

import com.lms.catalog.entity.CopyStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCopyDTO {

    private Long id;
    private Long bookId;

    @NotBlank(message = "Copy number is required")
    private String copyNumber;

    private String condition;
    private CopyStatus status;
}
