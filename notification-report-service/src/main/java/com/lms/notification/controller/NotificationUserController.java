package com.lms.notification.controller;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.NotificationDTO;
import com.lms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/notifications")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "User - Notifications", description = "User endpoints for notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationUserController {

    private final NotificationService notificationService;

    public NotificationUserController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get user's notifications")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotificationsByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notificationService.getByMember(memberId)));
    }

    @GetMapping("/{memberId}/unread")
    @Operation(summary = "Get user's unread notifications")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("Unread notifications retrieved", notificationService.getUnread(memberId)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", notificationService.markRead(id)));
    }

    @PutMapping("/{memberId}/read-all")
    @Operation(summary = "Mark all notifications as read for member")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@PathVariable Long memberId) {
        notificationService.markAllRead(memberId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }
}
