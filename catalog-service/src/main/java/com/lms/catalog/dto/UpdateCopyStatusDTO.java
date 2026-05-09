package com.lms.catalog.dto;

import com.lms.catalog.entity.CopyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCopyStatusDTO {

    @NotNull(message = "Status is required")
    private CopyStatus status;
}
