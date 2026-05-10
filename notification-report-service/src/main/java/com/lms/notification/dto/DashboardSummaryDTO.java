package com.lms.notification.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummaryDTO {
    private long totalBooks;
    private long totalMembers;
    private long activeMembers;
    private long totalActiveBorrows;
    private long totalOverdue;
    private double totalFinesUnpaid;
}
