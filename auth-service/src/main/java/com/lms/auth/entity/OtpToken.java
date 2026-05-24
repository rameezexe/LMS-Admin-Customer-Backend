package com.lms.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Builder.Default
    private boolean used = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.PASSWORD_RESET;

    /**
     * After the OTP is successfully verified for REGISTRATION, the row gets a
     * UUID verification token. The client passes this back on registration/initiate
     * to prove they own the email — without re-asking for the OTP.
     */
    @Column(length = 64)
    private String verificationToken;

    private LocalDateTime verifiedAt;
}
