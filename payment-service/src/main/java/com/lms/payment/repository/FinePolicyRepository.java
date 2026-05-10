package com.lms.payment.repository;

import com.lms.payment.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {
    Optional<FinePolicy> findTopByOrderByEffectiveDateDesc();
    List<FinePolicy> findAllByOrderByEffectiveDateDesc();
}
