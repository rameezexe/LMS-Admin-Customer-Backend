package com.lms.borrowing.controller;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.BorrowResponseDTO;
import com.lms.borrowing.security.SecurityHelper;
import com.lms.borrowing.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/borrows")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "User - Borrowing", description = "User endpoints for their own borrow records")
@SecurityRequirement(name = "bearerAuth")
public class BorrowUserController {

    private final BorrowService borrowService;
    private final SecurityHelper securityHelper;

    public BorrowUserController(BorrowService borrowService, SecurityHelper securityHelper) {
        this.borrowService = borrowService;
        this.securityHelper = securityHelper;
    }

    private void verifyOwnership(Long memberId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        if (!securityHelper.isAdmin() && (tokenMemberId == null || !tokenMemberId.equals(memberId))) {
            throw new AccessDeniedException("You can only access your own borrowing records.");
        }
    }

    @GetMapping("/active/{memberId}")
    @Operation(summary = "Get user's active borrows")
    public ResponseEntity<ApiResponse<List<BorrowResponseDTO>>> getActiveBorrows(@PathVariable Long memberId) {
        verifyOwnership(memberId);
        List<BorrowResponseDTO> response = borrowService.getActiveBorrows(memberId);
        return ResponseEntity.ok(ApiResponse.ok("Active borrows retrieved", response));
    }

    @GetMapping("/history/{memberId}")
    @Operation(summary = "Get user's complete borrow history")
    public ResponseEntity<ApiResponse<List<BorrowResponseDTO>>> getBorrowHistory(@PathVariable Long memberId) {
        verifyOwnership(memberId);
        List<BorrowResponseDTO> response = borrowService.getBorrowHistory(memberId);
        return ResponseEntity.ok(ApiResponse.ok("Borrow history retrieved", response));
    }

    @PostMapping("/return/{recordId}")
    @Operation(summary = "Return a book")
    public ResponseEntity<ApiResponse<BorrowResponseDTO>> returnBook(@PathVariable Long recordId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        BorrowResponseDTO response = borrowService.returnBook(recordId, tokenMemberId, securityHelper.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("Book returned successfully", response));
    }
}
