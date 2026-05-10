package com.lms.borrowing.controller;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.ReservationRequestDTO;
import com.lms.borrowing.dto.ReservationResponseDTO;
import com.lms.borrowing.security.SecurityHelper;
import com.lms.borrowing.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/borrows/reservations")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "User - Reservations", description = "User endpoints for book reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationUserController {

    private final ReservationService reservationService;
    private final SecurityHelper securityHelper;

    public ReservationUserController(ReservationService reservationService, SecurityHelper securityHelper) {
        this.reservationService = reservationService;
        this.securityHelper = securityHelper;
    }

    private void verifyOwnership(Long memberId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        if (!securityHelper.isAdmin() && (tokenMemberId == null || !tokenMemberId.equals(memberId))) {
            throw new AccessDeniedException("You can only access your own reservations.");
        }
    }

    @PostMapping
    @Operation(summary = "Create a reservation")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> createReservation(@Valid @RequestBody ReservationRequestDTO request) {
        verifyOwnership(request.getMemberId());
        ReservationResponseDTO response = reservationService.createReservation(request);
        return ResponseEntity.ok(ApiResponse.ok("Reservation created successfully", response));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get user's reservations")
    public ResponseEntity<ApiResponse<List<ReservationResponseDTO>>> getReservations(@PathVariable Long memberId) {
        verifyOwnership(memberId);
        List<ReservationResponseDTO> response = reservationService.getReservationsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.ok("Reservations retrieved", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify a pending reservation (Update Book ID)")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> modifyReservation(@PathVariable Long id, @RequestParam Long newBookId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        ReservationResponseDTO response = reservationService.modifyReservation(id, newBookId, tokenMemberId, securityHelper.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("Reservation modified successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        reservationService.cancelReservation(id, tokenMemberId, securityHelper.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled successfully", null));
    }
}
