package com.lms.member.controller;

import com.lms.member.dto.ApiResponse;
import com.lms.member.dto.MemberDTO;
import com.lms.member.dto.MemberRegistrationDTO;
import com.lms.member.dto.MemberStatsDTO;
import com.lms.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Members", description = "Admin endpoints for member management")
@SecurityRequirement(name = "bearerAuth")
public class MemberAdminController {

    private final MemberService memberService;

    public MemberAdminController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "Get all members (pageable)")
    public ResponseEntity<ApiResponse<Page<MemberDTO>>> getAllMembers(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<MemberDTO> members = memberService.getAllMembers(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Members retrieved successfully", members));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by ID")
    public ResponseEntity<ApiResponse<MemberDTO>> getMemberById(@PathVariable Long id) {
        MemberDTO member = memberService.getMemberById(id);
        return ResponseEntity.ok(ApiResponse.ok("Member retrieved successfully", member));
    }

    @GetMapping("/search")
    @Operation(summary = "Search members by name")
    public ResponseEntity<ApiResponse<Page<MemberDTO>>> searchMembers(
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<MemberDTO> members = memberService.searchMembers(name, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Search results", members));
    }

    @PostMapping
    @Operation(summary = "Register a new member manually")
    public ResponseEntity<ApiResponse<MemberDTO>> registerMember(@Valid @RequestBody MemberRegistrationDTO request) {
        MemberDTO member = memberService.registerMember(request);
        return ResponseEntity.ok(ApiResponse.ok("Member registered successfully", member));
    }

    @PostMapping(value = "/{memberId}/photo", consumes = "multipart/form-data")
    @Operation(summary = "Upload profile photo for a member")
    public ResponseEntity<ApiResponse<MemberDTO>> uploadProfilePhoto(
            @PathVariable Long memberId,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        MemberDTO member = memberService.uploadProfilePhoto(memberId, file, null, "ROLE_ADMIN");
        return ResponseEntity.ok(ApiResponse.ok("Profile photo uploaded successfully", member));
    }

    @DeleteMapping("/{memberId}/photo")
    @Operation(summary = "Remove profile photo for a member")
    public ResponseEntity<ApiResponse<Void>> removeProfilePhoto(@PathVariable Long memberId) {
        memberService.removeProfilePhoto(memberId, null, "ROLE_ADMIN");
        return ResponseEntity.ok(ApiResponse.ok("Profile photo removed successfully", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member profile")
    public ResponseEntity<ApiResponse<MemberDTO>> updateProfile(@PathVariable Long id, @RequestBody MemberRegistrationDTO request) {
        MemberDTO member = memberService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", member));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Suspend a member")
    public ResponseEntity<ApiResponse<Void>> deactivateMember(@PathVariable Long id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok(ApiResponse.ok("Member suspended successfully", null));
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a member")
    public ResponseEntity<ApiResponse<Void>> reactivateMember(@PathVariable Long id) {
        memberService.reactivateMember(id);
        return ResponseEntity.ok(ApiResponse.ok("Member reactivated successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete member (Soft delete: set to SUSPENDED)")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok(ApiResponse.ok("Member soft deleted (suspended) successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get member statistics")
    public ResponseEntity<ApiResponse<MemberStatsDTO>> getStats() {
        MemberStatsDTO stats = MemberStatsDTO.builder()
                .totalMembers(memberService.getTotalMembers())
                .activeMembers(memberService.getActiveMembers())
                .suspendedMembers(memberService.getSuspendedMembers())
                .premiumMembers(memberService.getPremiumMembers())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Stats retrieved successfully", stats));
    }
}
