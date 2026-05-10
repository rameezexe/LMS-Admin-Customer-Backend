package com.lms.notification.dto;

import com.lms.notification.entity.NotificationChannel;
import com.lms.notification.entity.NotificationStatus;
import com.lms.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDTO {
    private Long id;
    private Long memberId;
    private NotificationType type;
    private String message;
    private NotificationChannel channel;
    private NotificationStatus status;
    private boolean isRead;
    private LocalDateTime sentAt;
}
