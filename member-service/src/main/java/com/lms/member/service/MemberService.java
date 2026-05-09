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

import java.time.LocalDate;
import java.util.UUID;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
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
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .membershipType(dto.getMembershipType() != null ? dto.getMembershipType() : MembershipType.STANDARD)
                .status(MemberStatus.ACTIVE)
                .membershipNumber(membershipNumber)
                .joinDate(LocalDate.now())
                .build();

        return toDTO(memberRepository.save(member));
    }

    public MemberDTO getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + id));
        return toDTO(member);
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
        if (dto.getPhone() != null) member.setPhone(dto.getPhone());
        if (dto.getAddress() != null) member.setAddress(dto.getAddress());

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

    public long getTotalMembers() {
        return memberRepository.count();
    }

    public long getActiveMembers() {
        return memberRepository.countByStatus(MemberStatus.ACTIVE);
    }

    public long getSuspendedMembers() {
        return memberRepository.countByStatus(MemberStatus.SUSPENDED);
    }

    public long getPremiumMembers() {
        return memberRepository.countByMembershipType(MembershipType.PREMIUM);
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
                .build();
    }
}
