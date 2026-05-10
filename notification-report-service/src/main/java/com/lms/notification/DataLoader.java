package com.lms.notification;

import com.lms.notification.entity.Notification;
import com.lms.notification.entity.NotificationChannel;
import com.lms.notification.entity.NotificationStatus;
import com.lms.notification.entity.NotificationType;
import com.lms.notification.repository.NotificationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final NotificationRepository notificationRepository;

    public DataLoader(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (notificationRepository.count() == 0) {
            Notification n1 = Notification.builder()
                    .memberId(1L)
                    .type(NotificationType.BORROW_CONFIRMATION)
                    .message("You borrowed 'Effective Java'. Due: 2024-06-01")
                    .channel(NotificationChannel.IN_APP)
                    .status(NotificationStatus.SENT)
                    .isRead(false)
                    .sentAt(LocalDateTime.now().minusDays(2))
                    .build();
            notificationRepository.save(n1);

            Notification n2 = Notification.builder()
                    .memberId(1L)
                    .type(NotificationType.RETURN_REMINDER)
                    .message("Reminder: 'Clean Code' due on 2024-05-15")
                    .channel(NotificationChannel.EMAIL)
                    .status(NotificationStatus.SENT)
                    .isRead(false)
                    .sentAt(LocalDateTime.now().minusDays(1))
                    .build();
            notificationRepository.save(n2);

            Notification n3 = Notification.builder()
                    .memberId(1L)
                    .type(NotificationType.PAYMENT_RECEIPT)
                    .message("Payment ₹50.00 received. TxnID: 12345")
                    .channel(NotificationChannel.IN_APP)
                    .status(NotificationStatus.SENT)
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(n3);

            System.out.println("=== Notification DataLoader: Seeded Notifications ===");
        }
    }
}
