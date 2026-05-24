package com.lms.member.service;

import com.lms.member.dto.MemberDTO;
import com.lms.member.dto.MemberRegistrationDTO;
import com.lms.member.entity.Member;
import com.lms.member.entity.MemberStatus;
import com.lms.member.entity.MembershipType;
import com.lms.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final S3Service s3Service;
    private RestTemplate restTemplate;

    public MemberService(MemberRepository memberRepository, S3Service s3Service) {
        this.memberRepository = memberRepository;
        this.s3Service = s3Service;
    }

    // Optional setter for RestTemplate (injected when available)
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    public MemberDTO registerMember(MemberRegistrationDTO dto) {
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String membershipNumber = "LIB-" + uuid;

        Member member = Member.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone() != null ? dto.getPhone() : "")
                .address(dto.getAddress() != null ? dto.getAddress() : "")
                .membershipType(dto.getMembershipType() != null ? dto.getMembershipType() : MembershipType.STANDARD)
                .status(MemberStatus.PENDING_APPROVAL)
                .membershipNumber(membershipNumber)
                .joinDate(LocalDate.now())
                .governmentIdUrl(dto.getGovernmentIdUrl())
                .membershipDuration(dto.getMembershipDuration())
                .membershipAmount(dto.getMembershipAmount())
                .membershipPaymentId(dto.getMembershipPaymentId())
                .build();

        return toDTO(memberRepository.save(member));
    }

    public MemberDTO approveMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));

        if (member.getStatus() != MemberStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Member is not pending approval");
        }

        member.setStatus(MemberStatus.ACTIVE);

        // Calculate expiry date based on duration
        if (member.getMembershipDuration() != null) {
            LocalDate expiry;
            switch (member.getMembershipDuration()) {
                case "3_MONTHS": expiry = LocalDate.now().plusMonths(3); break;
                case "6_MONTHS": expiry = LocalDate.now().plusMonths(6); break;
                case "1_YEAR": default: expiry = LocalDate.now().plusYears(1); break;
            }
            member.setMembershipExpiryDate(expiry);
        }

        Member saved = memberRepository.save(member);

        // Send approval email notification
        sendApprovalEmail(saved.getEmail(), saved.getName());

        return toDTO(saved);
    }

    public MemberDTO declineMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));

        if (member.getStatus() != MemberStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Member is not pending approval");
        }

        member.setStatus(MemberStatus.DECLINED);
        Member saved = memberRepository.save(member);

        // Trigger refund via payment-service
        if (saved.getMembershipPaymentId() != null && restTemplate != null) {
            try {
                Map<String, String> refundRequest = Map.of("razorpayPaymentId", saved.getMembershipPaymentId());
                restTemplate.postForEntity(
                        "http://PAYMENT-SERVICE/api/internal/payments/refund",
                        refundRequest,
                        Object.class
                );
                System.out.println("[MemberService] Refund triggered for payment: " + saved.getMembershipPaymentId());
            } catch (Exception e) {
                System.err.println("[MemberService] Failed to trigger refund: " + e.getMessage());
            }
        }

        // Send decline email notification
        sendDeclineEmail(saved.getEmail(), saved.getName());

        return toDTO(saved);
    }

    public List<MemberDTO> getPendingMembers() {
        return memberRepository.findByStatus(MemberStatus.PENDING_APPROVAL)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public MemberDTO getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));
        return toDTO(member);
    }

    public MemberDTO uploadProfilePhoto(Long memberId, org.springframework.web.multipart.MultipartFile file, Long jwtMemberId, String jwtRole) {
        if ("ROLE_USER".equals(jwtRole)) {
            if (jwtMemberId == null || !jwtMemberId.equals(memberId)) {
                throw new org.springframework.security.access.AccessDeniedException("You can only upload your own photo");
            }
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

        if (member.getProfilePhotoUrl() != null) {
            s3Service.deleteFile(member.getProfilePhotoUrl());
        }

        String url = s3Service.uploadFile(file, "profile-photos/" + memberId);
        member.setProfilePhotoUrl(url);
        return toDTO(memberRepository.save(member));
    }

    public void removeProfilePhoto(Long memberId, Long jwtMemberId, String jwtRole) {
        if ("ROLE_USER".equals(jwtRole)) {
            if (jwtMemberId == null || !jwtMemberId.equals(memberId)) {
                throw new org.springframework.security.access.AccessDeniedException("You can only remove your own photo");
            }
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

        if (member.getProfilePhotoUrl() != null) {
            s3Service.deleteFile(member.getProfilePhotoUrl());
            member.setProfilePhotoUrl(null);
            memberRepository.save(member);
        }
    }

    public String uploadGovernmentId(org.springframework.web.multipart.MultipartFile file) {
        return s3Service.uploadFile(file, "government-ids");
    }

    public Page<MemberDTO> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<MemberDTO> searchMembers(String name, Pageable pageable) {
        return memberRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toDTO);
    }

    public MemberDTO updateProfile(Long id, MemberRegistrationDTO dto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));

        if (dto.getName() != null) member.setName(dto.getName());
        if (dto.getEmail() != null && !dto.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already in use by another member");
            }
            member.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) member.setPhone(dto.getPhone());
        if (dto.getAddress() != null) member.setAddress(dto.getAddress());
        if (dto.getMembershipType() != null) member.setMembershipType(dto.getMembershipType());

        return toDTO(memberRepository.save(member));
    }

    public void deactivateMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));
        member.setStatus(MemberStatus.SUSPENDED);
        memberRepository.save(member);
    }

    public void reactivateMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));
        member.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);
    }

    public long getTotalMembers() { return memberRepository.count(); }
    public long getActiveMembers() { return memberRepository.countByStatus(MemberStatus.ACTIVE); }
    public long getSuspendedMembers() { return memberRepository.countByStatus(MemberStatus.SUSPENDED); }
    public long getPremiumMembers() { return memberRepository.countByMembershipType(MembershipType.PREMIUM); }

    // ── Email Notifications ─────────────────────────────────────────────

    private void sendApprovalEmail(String email, String name) {
        if (restTemplate == null) return;
        try {
            Map<String, String> request = Map.of(
                    "email", email,
                    "otp", "APPROVED: " + name + ", your library membership has been approved! You can now log in."
            );
            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/otp",
                    request, Object.class
            );
        } catch (Exception e) {
            System.err.println("[MemberService] Failed to send approval email: " + e.getMessage());
        }
    }

    private void sendDeclineEmail(String email, String name) {
        if (restTemplate == null) return;
        try {
            Map<String, String> request = Map.of(
                    "email", email,
                    "otp", "DECLINED: " + name + ", your library membership application was declined. Your payment will be refunded."
            );
            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/otp",
                    request, Object.class
            );
        } catch (Exception e) {
            System.err.println("[MemberService] Failed to send decline email: " + e.getMessage());
        }
    }

    public MemberDTO toDTO(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .address(member.getAddress())
                .membershipNumber(member.getMembershipNumber())
                .membershipType(member.getMembershipType())
                .status(member.getStatus())
                .joinDate(member.getJoinDate())
                .profilePhotoUrl(member.getProfilePhotoUrl())
                .governmentIdUrl(member.getGovernmentIdUrl())
                .membershipDuration(member.getMembershipDuration())
                .membershipAmount(member.getMembershipAmount())
                .membershipPaymentId(member.getMembershipPaymentId())
                .membershipExpiryDate(member.getMembershipExpiryDate())
                .build();
    }
}
