package com.lms.member.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberStatsDTO {
    private long totalMembers;
    private long activeMembers;
    private long suspendedMembers;
    private long premiumMembers;
}
