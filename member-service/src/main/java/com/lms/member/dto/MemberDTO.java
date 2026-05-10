package com.lms.member.dto;

import com.lms.member.entity.MemberStatus;
import com.lms.member.entity.MembershipType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MemberDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String membershipNumber;
    private MembershipType membershipType;
    private MemberStatus status;
    private LocalDate joinDate;
    private String profilePhotoUrl;
}
