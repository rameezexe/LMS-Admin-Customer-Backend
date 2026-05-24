package com.lms.notification.service;

import com.lms.notification.dto.NotificationDTO;
import com.lms.notification.entity.Notification;
import com.lms.notification.entity.NotificationChannel;
import com.lms.notification.entity.NotificationStatus;
import com.lms.notification.entity.NotificationType;
import com.lms.notification.repository.NotificationRepository;
import com.lms.notification.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;
    private final SecurityHelper securityHelper;

    public NotificationService(NotificationRepository notificationRepository, SecurityHelper securityHelper) {
        this.notificationRepository = notificationRepository;
        this.securityHelper = securityHelper;
    }

    private NotificationDTO mapToDTO(Notification notif) {
        return NotificationDTO.builder()
                .id(notif.getId())
                .memberId(notif.getMemberId())
                .type(notif.getType())
                .message(notif.getMessage())
                .channel(notif.getChannel())
                .status(notif.getStatus())
                .isRead(notif.isRead())
                .sentAt(notif.getSentAt())
                .build();
    }

    private void verifyOwnership(Long memberId) {
        Long tokenMemberId = securityHelper.extractMemberIdFromContext();
        if (!securityHelper.isAdmin() && (tokenMemberId == null || !tokenMemberId.equals(memberId))) {
            throw new AccessDeniedException("You can only access your own notifications.");
        }
    }

    private NotificationDTO createAndSend(Long memberId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .type(type)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SENT)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("[NOTIF] {} -> memberId {}", type, memberId);
        return mapToDTO(saved);
    }

    @Transactional
    public NotificationDTO sendBorrowConfirmation(Long memberId, String bookTitle, String dueDate) {
        String message = "You borrowed '" + bookTitle + "'. Due: " + dueDate;
        return createAndSend(memberId, NotificationType.BORROW_CONFIRMATION, message);
    }

    @Transactional
    public NotificationDTO sendReturnReminder(Long memberId, String bookTitle, String dueDate) {
        String message = "Reminder: '" + bookTitle + "' due on " + dueDate;
        return createAndSend(memberId, NotificationType.RETURN_REMINDER, message);
    }

    @Transactional
    public NotificationDTO sendOverdueAlert(Long memberId, String bookTitle, BigDecimal fineAmount) {
        String message = "'" + bookTitle + "' is overdue. Fine: ₹" + fineAmount;
        return createAndSend(memberId, NotificationType.OVERDUE_ALERT, message);
    }

    @Transactional
    public NotificationDTO sendPaymentReceipt(Long memberId, BigDecimal amount, String transactionId) {
        String message = "Payment ₹" + amount + " received. TxnID: " + transactionId;
        return createAndSend(memberId, NotificationType.PAYMENT_RECEIPT, message);
    }

    @Transactional
    public NotificationDTO sendReservationConfirmation(Long memberId, String bookTitle) {
        String message = "Reservation confirmed for '" + bookTitle + "'. We'll notify you when a copy is ready for pickup.";
        return createAndSend(memberId, NotificationType.RESERVATION_ALERT, message);
    }

    @Transactional
    public NotificationDTO sendReservationReady(Long memberId, String bookTitle, Integer pickupWindowDays) {
        int days = pickupWindowDays == null ? 3 : pickupWindowDays;
        String message = "'" + bookTitle + "' is ready for pickup. Please collect within " + days + " day(s).";
        return createAndSend(memberId, NotificationType.RESERVATION_ALERT, message);
    }

    public List<NotificationDTO> getByMember(Long memberId) {
        verifyOwnership(memberId);
        return notificationRepository.findByMemberId(memberId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnread(Long memberId) {
        verifyOwnership(memberId);
        return notificationRepository.findByMemberIdAndIsRead(memberId, false).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDTO markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        verifyOwnership(notification.getMemberId());
        
        notification.setRead(true);
        return mapToDTO(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long memberId) {
        verifyOwnership(memberId);
        List<Notification> unread = notificationRepository.findByMemberIdAndIsRead(memberId, false);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        log.info("[NOTIF-SCHEDULER] Sending daily return reminders");
        // In a real system, this would call borrowing-service to find books due tomorrow
        // and loop through them to call sendReturnReminder
    }
}
