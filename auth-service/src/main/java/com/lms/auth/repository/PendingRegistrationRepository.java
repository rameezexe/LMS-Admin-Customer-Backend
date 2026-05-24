package com.lms.auth.repository;

import com.lms.auth.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByPendingId(String pendingId);

    Optional<PendingRegistration> findByEmail(String email);

    Optional<PendingRegistration> findByRazorpayOrderId(String razorpayOrderId);

    List<PendingRegistration> findByExpiresAtBefore(LocalDateTime cutoff);

    void deleteByEmail(String email);
}
