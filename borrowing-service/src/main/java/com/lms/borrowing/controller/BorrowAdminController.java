package com.lms.borrowing.controller;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.BorrowRequestDTO;
import com.lms.borrowing.dto.BorrowResponseDTO;
import com.lms.borrowing.dto.BorrowStatsDTO;
import com.lms.borrowing.service.BorrowService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/borrows")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Borrowing", description = "Admin endpoints for borrowing management")
@SecurityRequirement(name = "bearerAuth")
public class BorrowAdminController {

    private final BorrowService borrowService;

    public BorrowAdminController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping("/issue")
    @Operation(summary = "Issue a book to a member")
    public ResponseEntity<ApiResponse<BorrowResponseDTO>> issueBook(@Valid @RequestBody BorrowRequestDTO request) {
        BorrowResponseDTO response = borrowService.issueBook(request);
        return ResponseEntity.ok(ApiResponse.ok("Book issued successfully", response));
    }

    @PostMapping("/return/{recordId}")
    @Operation(summary = "Return a book (Admin override)")
    public ResponseEntity<ApiResponse<BorrowResponseDTO>> returnBookAdmin(@PathVariable Long recordId) {
        BorrowResponseDTO response = borrowService.returnBook(recordId, null, true);
        return ResponseEntity.ok(ApiResponse.ok("Book returned successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all borrow records (pageable)")
    public ResponseEntity<ApiResponse<Page<BorrowResponseDTO>>> getAllBorrows(@PageableDefault(size = 10) Pageable pageable) {
        Page<BorrowResponseDTO> response = borrowService.adminGetAllBorrows(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Records retrieved", response));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get all overdue books")
    public ResponseEntity<ApiResponse<List<BorrowResponseDTO>>> getAllOverdue() {
        List<BorrowResponseDTO> response = borrowService.getAllOverdue();
        return ResponseEntity.ok(ApiResponse.ok("Overdue records retrieved", response));
    }

    @PutMapping("/{recordId}/waive-fine")
    @Operation(summary = "Waive fine for a borrow record")
    public ResponseEntity<ApiResponse<BorrowResponseDTO>> waiveFine(@PathVariable Long recordId) {
        BorrowResponseDTO response = borrowService.waiveFine(recordId);
        return ResponseEntity.ok(ApiResponse.ok("Fine waived successfully", response));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get borrow history for any member")
    public ResponseEntity<ApiResponse<List<BorrowResponseDTO>>> getMemberBorrowHistory(@PathVariable Long memberId) {
        List<BorrowResponseDTO> response = borrowService.getBorrowHistory(memberId);
        return ResponseEntity.ok(ApiResponse.ok("Member history retrieved", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get borrowing statistics")
    public ResponseEntity<ApiResponse<BorrowStatsDTO>> getStats() {
        BorrowStatsDTO stats = BorrowStatsDTO.builder()
                .totalActive(borrowService.getTotalActive())
                .totalOverdue(borrowService.getTotalOverdue())
                .totalReturned(borrowService.getTotalReturned())
                .totalFinesUnpaid(borrowService.getTotalFinesUnpaid())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Stats retrieved", stats));
    }
}
