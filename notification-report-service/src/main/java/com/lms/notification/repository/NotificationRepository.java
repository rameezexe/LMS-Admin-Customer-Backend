package com.lms.notification.repository;

import com.lms.notification.entity.Notification;
import com.lms.notification.entity.NotificationStatus;
import com.lms.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMemberId(Long memberId);
    List<Notification> findByMemberIdAndIsRead(Long memberId, boolean isRead);
    List<Notification> findByMemberIdAndType(Long memberId, NotificationType type);
    List<Notification> findByStatus(NotificationStatus status);
    long countByMemberIdAndIsRead(Long memberId, boolean isRead);
}
