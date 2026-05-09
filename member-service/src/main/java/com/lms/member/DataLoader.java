package com.lms.member;

import com.lms.member.dto.MemberRegistrationDTO;
import com.lms.member.entity.MembershipType;
import com.lms.member.repository.MemberRepository;
import com.lms.member.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    public DataLoader(MemberService memberService, MemberRepository memberRepository) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (memberRepository.count() == 0) {
            
            // Seed 3 Active Standard Members
            memberService.registerMember(createDto("User One", "user1@example.com", "555-1001", MembershipType.STANDARD));
            memberService.registerMember(createDto("User Two", "user2@example.com", "555-1002", MembershipType.STANDARD));
            memberService.registerMember(createDto("User Three", "user3@example.com", "555-1003", MembershipType.STANDARD));
            
            // Seed 1 Premium Member
            memberService.registerMember(createDto("User Four", "user4@example.com", "555-1004", MembershipType.PREMIUM));
            
            System.out.println("=== Member DataLoader: Seeded 4 members ===");
        }
    }

    private MemberRegistrationDTO createDto(String name, String email, String phone, MembershipType type) {
        MemberRegistrationDTO dto = new MemberRegistrationDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setAddress("123 Library St");
        dto.setMembershipType(type);
        return dto;
    }
}
