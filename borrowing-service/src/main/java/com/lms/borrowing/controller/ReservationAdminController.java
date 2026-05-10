package com.lms.borrowing.controller;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.ReservationResponseDTO;
import com.lms.borrowing.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/borrows/reservations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reservations", description = "Admin endpoints for reservation management")
@SecurityRequirement(name = "bearerAuth")
public class ReservationAdminController {

    private final ReservationService reservationService;

    public ReservationAdminController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Get all reservations (pageable)")
    public ResponseEntity<ApiResponse<Page<ReservationResponseDTO>>> getAllReservations(@PageableDefault(size = 10) Pageable pageable) {
        Page<ReservationResponseDTO> response = reservationService.getAllReservations(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Reservations retrieved", response));
    }

    @PutMapping("/{id}/fulfill")
    @Operation(summary = "Fulfill a reservation")
    public ResponseEntity<ApiResponse<Void>> fulfillReservation(@PathVariable Long id) {
        reservationService.fulfillReservation(id);
        return ResponseEntity.ok(ApiResponse.ok("Reservation fulfilled successfully", null));
    }
}
