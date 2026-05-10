package com.lms.borrowing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BorrowStatsDTO {
    private long totalActive;
    private long totalOverdue;
    private long totalReturned;
    private long totalFinesUnpaid;
}
