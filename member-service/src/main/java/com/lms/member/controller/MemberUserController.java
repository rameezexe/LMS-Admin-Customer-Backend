package com.lms.member.controller;

import com.lms.member.dto.ApiResponse;
import com.lms.member.dto.MemberDTO;
import com.lms.member.dto.MemberRegistrationDTO;
import com.lms.member.security.SecurityHelper;
import com.lms.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "User - Profile", description = "User profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MemberUserController {

    private final MemberService memberService;
    private final SecurityHelper securityHelper;

    public MemberUserController(MemberService memberService, SecurityHelper securityHelper) {
        this.memberService = memberService;
        this.securityHelper = securityHelper;
    }

    private void verifyOwnership(Long memberId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        if (!securityHelper.isAdmin() && (tokenMemberId == null || !tokenMemberId.equals(memberId))) {
            throw new AccessDeniedException("You can only access your own profile");
        }
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get user's own profile")
    public ResponseEntity<ApiResponse<MemberDTO>> getMyProfile(@PathVariable Long memberId) {
        verifyOwnership(memberId);
        MemberDTO member = memberService.getMemberById(memberId);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", member));
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "Update user's own profile")
    public ResponseEntity<ApiResponse<MemberDTO>> updateMyProfile(@PathVariable Long memberId, @RequestBody MemberRegistrationDTO request) {
        verifyOwnership(memberId);
        MemberDTO member = memberService.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", member));
    }

    @GetMapping("/{memberId}/membership")
    @Operation(summary = "Get user's membership details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMembershipDetails(@PathVariable Long memberId) {
        verifyOwnership(memberId);
        MemberDTO member = memberService.getMemberById(memberId);
        
        Map<String, Object> details = Map.of(
                "membershipNumber", member.getMembershipNumber(),
                "membershipType", member.getMembershipType(),
                "status", member.getStatus(),
                "joinDate", member.getJoinDate()
        );
        
        return ResponseEntity.ok(ApiResponse.ok("Membership details retrieved successfully", details));
    }
}
