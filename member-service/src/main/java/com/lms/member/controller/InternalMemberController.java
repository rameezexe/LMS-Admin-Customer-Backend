package com.lms.member.controller;

import com.lms.member.dto.ApiResponse;
import com.lms.member.dto.MemberDTO;
import com.lms.member.dto.MemberRegistrationDTO;
import com.lms.member.entity.Member;
import com.lms.member.repository.MemberRepository;
import com.lms.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/members")
@Tag(name = "Internal - Members", description = "Internal endpoints for microservice communication")
public class InternalMemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    public InternalMemberController(MemberRepository memberRepository, MemberService memberService) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
    }

    @GetMapping("/by-email")
    @Operation(summary = "Find member by email (internal use)")
    public ResponseEntity<ApiResponse<MemberDTO>> findByEmail(@RequestParam String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No member found with email: " + email));
        return ResponseEntity.ok(ApiResponse.ok("Member found", memberService.toDTO(member)));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check if email is already registered")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        boolean exists = memberRepository.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Email check complete", Map.of("exists", exists)));
    }

    @PostMapping
    @Operation(summary = "Create member (internal use, called during registration)")
    public ResponseEntity<ApiResponse<MemberDTO>> createMember(@RequestBody MemberRegistrationDTO request) {
        MemberDTO member = memberService.registerMember(request);
        return ResponseEntity.ok(ApiResponse.ok("Member created", member));
    }

    @PostMapping(value = "/upload-doc", consumes = "multipart/form-data")
    @Operation(summary = "Upload government ID document to S3 (no auth required)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(
            @RequestPart("file") MultipartFile file) {
        String url = memberService.uploadGovernmentId(file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", Map.of("url", url)));
    }
}
