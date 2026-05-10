package com.lms.borrowing.dto;

import lombok.Data;

@Data
public class MemberDTO {
    private Long id;
    private String name;
    private String email;
    private String status;
}
