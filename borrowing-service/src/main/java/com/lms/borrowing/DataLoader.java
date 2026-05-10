package com.lms.borrowing;

import com.lms.borrowing.entity.BorrowRecord;
import com.lms.borrowing.entity.BorrowStatus;
import com.lms.borrowing.entity.Reservation;
import com.lms.borrowing.entity.ReservationStatus;
import com.lms.borrowing.repository.BorrowRecordRepository;
import com.lms.borrowing.repository.ReservationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {

    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;

    public DataLoader(BorrowRecordRepository borrowRecordRepository, ReservationRepository reservationRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (borrowRecordRepository.count() == 0) {
            
            // Member 1: 1 Active, 1 Overdue
            borrowRecordRepository.save(BorrowRecord.builder()
                    .memberId(1L).bookCopyId(101L).bookId(10L)
                    .issueDate(LocalDate.now().minusDays(5))
                    .dueDate(LocalDate.now().plusDays(9))
                    .status(BorrowStatus.ACTIVE)
                    .build());

            borrowRecordRepository.save(BorrowRecord.builder()
                    .memberId(1L).bookCopyId(102L).bookId(11L)
                    .issueDate(LocalDate.now().minusDays(20))
                    .dueDate(LocalDate.now().minusDays(6))
                    .status(BorrowStatus.OVERDUE)
                    .fineAmount(BigDecimal.valueOf(30.00)) // 6 days * 5
                    .build());

            // Member 2: 1 Active
            borrowRecordRepository.save(BorrowRecord.builder()
                    .memberId(2L).bookCopyId(103L).bookId(12L)
                    .issueDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(14))
                    .status(BorrowStatus.ACTIVE)
                    .build());

            System.out.println("=== Borrowing DataLoader: Seeded 3 Borrow Records ===");
        }

        if (reservationRepository.count() == 0) {
            reservationRepository.save(Reservation.builder()
                    .memberId(1L).bookId(15L)
                    .reservationDate(LocalDate.now())
                    .expiryDate(LocalDate.now().plusDays(3))
                    .status(ReservationStatus.PENDING)
                    .build());
                    
            System.out.println("=== Borrowing DataLoader: Seeded 1 Reservation ===");
        }
    }
}
