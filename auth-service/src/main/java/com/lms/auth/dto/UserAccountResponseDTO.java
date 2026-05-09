package com.lms.auth.dto;

import com.lms.auth.entity.Role;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccountResponseDTO {
    private Long id;
    private String username;
    private Role role;
    private Long memberId;
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
