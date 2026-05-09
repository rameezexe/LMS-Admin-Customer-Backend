package com.lms.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private Long userAccountId;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(name = "is_revoked")
    private boolean revoked = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
