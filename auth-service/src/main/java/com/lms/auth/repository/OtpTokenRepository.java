package com.lms.auth.repository;

import com.lms.auth.entity.OtpPurpose;
import com.lms.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndUsedFalseOrderByExpiryTimeDesc(String email);

    Optional<OtpToken> findTopByEmailAndPurposeAndUsedFalseOrderByExpiryTimeDesc(String email, OtpPurpose purpose);

    Optional<OtpToken> findByEmailAndVerificationTokenAndPurpose(String email, String verificationToken, OtpPurpose purpose);
}
