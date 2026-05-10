package com.lms.payment.service;

import com.lms.payment.dto.FinePolicyDTO;
import com.lms.payment.dto.FinePolicyUpdateDTO;
import com.lms.payment.entity.FinePolicy;
import com.lms.payment.repository.FinePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinePolicyService {

    private final FinePolicyRepository finePolicyRepository;

    public FinePolicyService(FinePolicyRepository finePolicyRepository) {
        this.finePolicyRepository = finePolicyRepository;
    }

    private FinePolicyDTO mapToDTO(FinePolicy policy) {
        return FinePolicyDTO.builder()
                .id(policy.getId())
                .finePerDay(policy.getFinePerDay())
                .gracePeriodDays(policy.getGracePeriodDays())
                .maxFineAmount(policy.getMaxFineAmount())
                .effectiveDate(policy.getEffectiveDate())
                .build();
    }

    public FinePolicyDTO getCurrentPolicy() {
        FinePolicy policy = finePolicyRepository.findTopByOrderByEffectiveDateDesc()
                .orElseThrow(() -> new RuntimeException("No policy configured"));
        return mapToDTO(policy);
    }

    @Transactional
    public FinePolicyDTO updatePolicy(FinePolicyUpdateDTO request) {
        FinePolicy newPolicy = FinePolicy.builder()
                .finePerDay(request.getFinePerDay())
                .gracePeriodDays(request.getGracePeriodDays())
                .maxFineAmount(request.getMaxFineAmount())
                .effectiveDate(LocalDate.now())
                .build();

        return mapToDTO(finePolicyRepository.save(newPolicy));
    }

    public List<FinePolicyDTO> getPolicyHistory() {
        return finePolicyRepository.findAllByOrderByEffectiveDateDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
