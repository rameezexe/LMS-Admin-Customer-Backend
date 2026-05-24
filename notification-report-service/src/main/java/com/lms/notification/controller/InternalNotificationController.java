package com.lms.notification.controller;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.BorrowConfirmationRequestDTO;
import com.lms.notification.dto.NotificationDTO;
import com.lms.notification.dto.OtpEmailRequestDTO;
import com.lms.notification.dto.ReservationConfirmationRequestDTO;
import com.lms.notification.dto.ReservationReadyRequestDTO;
import com.lms.notification.dto.OverdueAlertRequestDTO;
import com.lms.notification.dto.PaymentReceiptRequestDTO;
import com.lms.notification.service.EmailService;
import com.lms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/notifications")
@Tag(name = "Internal - Notifications", description = "Internal endpoints for microservice communication")
public class InternalNotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;

    public InternalNotificationController(NotificationService notificationService, EmailService emailService) {
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @PostMapping("/send/borrow")
    @Operation(summary = "Send borrow confirmation internally")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendBorrowConfirmation(@Valid @RequestBody BorrowConfirmationRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Borrow confirmation sent",
                notificationService.sendBorrowConfirmation(request.getMemberId(), request.getBookTitle(), request.getDueDate())));
    }

    @PostMapping("/send/overdue")
    @Operation(summary = "Send overdue alert internally")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendOverdueAlert(@Valid @RequestBody OverdueAlertRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Overdue alert sent",
                notificationService.sendOverdueAlert(request.getMemberId(), request.getBookTitle(), request.getFineAmount())));
    }

    @PostMapping("/send/payment")
    @Operation(summary = "Send payment receipt internally")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendPaymentReceipt(@Valid @RequestBody PaymentReceiptRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Payment receipt sent",
                notificationService.sendPaymentReceipt(request.getMemberId(), request.getAmount(), request.getTransactionId())));
    }

    @PostMapping("/send/otp")
    @Operation(summary = "Send OTP email for password reset")
    public ResponseEntity<ApiResponse<Void>> sendOtpEmail(@Valid @RequestBody OtpEmailRequestDTO request) {
        emailService.sendOtpEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("OTP email sent", null));
    }

    @PostMapping("/send/reservation")
    @Operation(summary = "Send reservation confirmation internally")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendReservationConfirmation(@Valid @RequestBody ReservationConfirmationRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation confirmation sent",
                notificationService.sendReservationConfirmation(request.getMemberId(), request.getBookTitle())));
    }

    @PostMapping("/send/reservation-ready")
    @Operation(summary = "Send reservation-ready-for-pickup notification internally")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendReservationReady(@Valid @RequestBody ReservationReadyRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation ready notification sent",
                notificationService.sendReservationReady(request.getMemberId(), request.getBookTitle(), request.getPickupWindowDays())));
    }
}
