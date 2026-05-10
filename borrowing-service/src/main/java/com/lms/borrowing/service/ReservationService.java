package com.lms.borrowing.service;

import com.lms.borrowing.dto.ReservationRequestDTO;
import com.lms.borrowing.dto.ReservationResponseDTO;
import com.lms.borrowing.entity.Reservation;
import com.lms.borrowing.entity.ReservationStatus;
import com.lms.borrowing.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public ReservationResponseDTO createReservation(ReservationRequestDTO request) {
        reservationRepository.findByMemberIdAndBookIdAndStatus(request.getMemberId(), request.getBookId(), ReservationStatus.PENDING)
                .ifPresent(r -> {
                    throw new IllegalStateException("You already have a pending reservation for this book.");
                });

        Reservation reservation = Reservation.builder()
                .memberId(request.getMemberId())
                .bookId(request.getBookId())
                .reservationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(3))
                .status(ReservationStatus.PENDING)
                .build();

        return toDTO(reservationRepository.save(reservation));
    }

    public void cancelReservation(Long id, Long requestMemberId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));

        if (!isAdmin && !reservation.getMemberId().equals(requestMemberId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only cancel your own reservations.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public ReservationResponseDTO modifyReservation(Long id, Long newBookId, Long requestMemberId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));

        if (!isAdmin && !reservation.getMemberId().equals(requestMemberId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own reservations.");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be modified.");
        }

        reservation.setBookId(newBookId);
        return toDTO(reservationRepository.save(reservation));
    }

    public void fulfillReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
    }

    public List<ReservationResponseDTO> getReservationsByMember(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Page<ReservationResponseDTO> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable).map(this::toDTO);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void expireReservations() {
        List<Reservation> pending = reservationRepository.findByExpiryDateBeforeAndStatus(LocalDate.now(), ReservationStatus.PENDING);
        for (Reservation reservation : pending) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
        }
    }

    private ReservationResponseDTO toDTO(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .memberId(reservation.getMemberId())
                .bookId(reservation.getBookId())
                .reservationDate(reservation.getReservationDate())
                .expiryDate(reservation.getExpiryDate())
                .status(reservation.getStatus())
                .build();
    }
}
