package com.lms.borrowing.repository;

import com.lms.borrowing.entity.Reservation;
import com.lms.borrowing.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberId(Long memberId);

    Optional<Reservation> findByBookIdAndStatus(Long bookId, ReservationStatus status);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByExpiryDateBeforeAndStatus(LocalDate date, ReservationStatus status);
    
    Optional<Reservation> findByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, ReservationStatus status);
}
