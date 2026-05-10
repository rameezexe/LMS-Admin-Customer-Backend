package com.lms.notification.controller;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.BorrowConfirmationRequestDTO;
import com.lms.notification.dto.NotificationDTO;
import com.lms.notification.dto.OverdueAlertRequestDTO;
import com.lms.notification.dto.PaymentReceiptRequestDTO;
import com.lms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Notifications", description = "Admin endpoints for notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationAdminController {

    private final NotificationService notificationService;

    public NotificationAdminController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all notifications")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getAllNotifications(Pageable pageable) {
        List<NotificationDTO> all = notificationService.getAllNotifications();
        // In real app, use repository pageable directly. Wrapping list for simplicity here.
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), all.size());
        Page<NotificationDTO> page = new PageImpl<>(all.subList(start, end), pageable, all.size());
        return ResponseEntity.ok(ApiResponse.ok("All notifications retrieved", page));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get notifications for any member")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByMember(@PathVariable Long memberId) {
        // Admin can fetch without verifyOwnership checking JWT memberId vs requested
        // Let's create a dedicated method or just bypass ownership locally if we change service logic
        // For now, service logic checks ownership. We'll let service check ownership and since it's ADMIN, it will pass.
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notificationService.getByMember(memberId)));
    }

    @PostMapping("/send/borrow")
    @Operation(summary = "Send borrow confirmation")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendBorrowConfirmation(@Valid @RequestBody BorrowConfirmationRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Borrow confirmation sent",
                notificationService.sendBorrowConfirmation(request.getMemberId(), request.getBookTitle(), request.getDueDate())));
    }

    @PostMapping("/send/overdue")
    @Operation(summary = "Send overdue alert")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendOverdueAlert(@Valid @RequestBody OverdueAlertRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Overdue alert sent",
                notificationService.sendOverdueAlert(request.getMemberId(), request.getBookTitle(), request.getFineAmount())));
    }

    @PostMapping("/send/payment")
    @Operation(summary = "Send payment receipt")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendPaymentReceipt(@Valid @RequestBody PaymentReceiptRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Payment receipt sent",
                notificationService.sendPaymentReceipt(request.getMemberId(), request.getAmount(), request.getTransactionId())));
    }
}
