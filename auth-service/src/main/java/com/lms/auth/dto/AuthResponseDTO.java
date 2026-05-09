package com.lms.auth.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Long memberId;
    private long expiresIn;
}
