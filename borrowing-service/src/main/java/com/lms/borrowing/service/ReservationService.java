package com.lms.borrowing.service;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.BookDTO;
import com.lms.borrowing.dto.ReservationRequestDTO;
import com.lms.borrowing.dto.ReservationResponseDTO;
import com.lms.borrowing.entity.Reservation;
import com.lms.borrowing.entity.ReservationStatus;
import com.lms.borrowing.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final int MAX_ACTIVE_RESERVATIONS_PER_MEMBER = 3;
    private static final int PICKUP_WINDOW_DAYS = 3;
    private static final EnumSet<ReservationStatus> ACTIVE_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.READY_FOR_PICKUP);

    private final ReservationRepository reservationRepository;
    private final RestTemplate restTemplate;

    public ReservationService(ReservationRepository reservationRepository, RestTemplate restTemplate) {
        this.reservationRepository = reservationRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO request) {
        reservationRepository.findFirstByMemberIdAndBookIdAndStatusIn(
                        request.getMemberId(), request.getBookId(), ACTIVE_STATUSES)
                .ifPresent(r -> {
                    throw new IllegalStateException("You already have an active reservation for this book.");
                });

        long activeForMember = reservationRepository.countByMemberIdAndStatusIn(request.getMemberId(), ACTIVE_STATUSES);
        if (activeForMember >= MAX_ACTIVE_RESERVATIONS_PER_MEMBER) {
            throw new IllegalStateException(
                    "Reservation limit reached (max " + MAX_ACTIVE_RESERVATIONS_PER_MEMBER + " active reservations).");
        }

        BookDTO book = fetchBook(request.getBookId());

        int queuePosition = (int) reservationRepository.countByBookIdAndStatusIn(request.getBookId(), ACTIVE_STATUSES) + 1;

        Reservation reservation = Reservation.builder()
                .memberId(request.getMemberId())
                .bookId(request.getBookId())
                .reservationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(PICKUP_WINDOW_DAYS))
                .status(ReservationStatus.PENDING)
                .queuePosition(queuePosition)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        sendReservationNotification(request.getMemberId(), book.getTitle());

        return toDTO(saved);
    }

    @Transactional
    public void cancelReservation(Long id, Long requestMemberId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));

        if (!isAdmin && !reservation.getMemberId().equals(requestMemberId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only cancel your own reservations.");
        }

        ReservationStatus prevStatus = reservation.getStatus();
        Long bookId = reservation.getBookId();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        if (ACTIVE_STATUSES.contains(prevStatus)) {
            resequenceQueue(bookId);
            if (prevStatus == ReservationStatus.READY_FOR_PICKUP) {
                promoteNextReservation(bookId);
            }
        }
    }

    @Transactional
    public ReservationResponseDTO modifyReservation(Long id, Long newBookId, Long requestMemberId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));

        if (!isAdmin && !reservation.getMemberId().equals(requestMemberId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only modify your own reservations.");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be modified.");
        }

        if (reservation.getBookId().equals(newBookId)) {
            return toDTO(reservation);
        }

        reservationRepository.findFirstByMemberIdAndBookIdAndStatusIn(
                        reservation.getMemberId(), newBookId, ACTIVE_STATUSES)
                .ifPresent(r -> {
                    throw new IllegalStateException("You already have an active reservation for the target book.");
                });

        fetchBook(newBookId);

        Long oldBookId = reservation.getBookId();
        int newQueuePosition = (int) reservationRepository.countByBookIdAndStatusIn(newBookId, ACTIVE_STATUSES) + 1;

        reservation.setBookId(newBookId);
        reservation.setQueuePosition(newQueuePosition);
        Reservation saved = reservationRepository.save(reservation);

        resequenceQueue(oldBookId);

        return toDTO(saved);
    }

    @Transactional
    public void fulfillReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found."));
        Long bookId = reservation.getBookId();
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
        resequenceQueue(bookId);
        promoteNextReservation(bookId);
    }

    /**
     * Called by BorrowService when a copy of `bookId` becomes available.
     * Promotes the oldest PENDING reservation to READY_FOR_PICKUP and notifies the member.
     * No-op if there is already a READY_FOR_PICKUP holder or the queue is empty.
     */
    @Transactional
    public void promoteNextReservation(Long bookId) {
        boolean alreadyReady = reservationRepository
                .findFirstByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.READY_FOR_PICKUP)
                .isPresent();
        if (alreadyReady) return;

        reservationRepository
                .findFirstByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.PENDING)
                .ifPresent(next -> {
                    next.setStatus(ReservationStatus.READY_FOR_PICKUP);
                    next.setReadyAt(LocalDateTime.now());
                    next.setExpiryDate(LocalDate.now().plusDays(PICKUP_WINDOW_DAYS));
                    reservationRepository.save(next);

                    String bookTitle = safeFetchBookTitle(bookId);
                    sendReservationReadyNotification(next.getMemberId(), bookTitle);
                });
    }

    public List<ReservationResponseDTO> getReservationsByMember(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Page<ReservationResponseDTO> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable).map(this::toDTO);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireReservations() {
        List<Reservation> expiredPending = reservationRepository.findByExpiryDateBeforeAndStatus(
                LocalDate.now(), ReservationStatus.PENDING);
        for (Reservation reservation : expiredPending) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            resequenceQueue(reservation.getBookId());
        }

        List<Reservation> expiredReady = reservationRepository.findByExpiryDateBeforeAndStatus(
                LocalDate.now(), ReservationStatus.READY_FOR_PICKUP);
        for (Reservation reservation : expiredReady) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            resequenceQueue(reservation.getBookId());
            promoteNextReservation(reservation.getBookId());
        }
    }

    private void resequenceQueue(Long bookId) {
        List<Reservation> active = reservationRepository.findByStatus(ReservationStatus.PENDING).stream()
                .filter(r -> r.getBookId().equals(bookId))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(
                            a.getQueuePosition() == null ? Integer.MAX_VALUE : a.getQueuePosition(),
                            b.getQueuePosition() == null ? Integer.MAX_VALUE : b.getQueuePosition());
                    if (cmp != 0) return cmp;
                    return a.getReservationDate().compareTo(b.getReservationDate());
                })
                .collect(Collectors.toList());

        int offset = reservationRepository
                .findFirstByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.READY_FOR_PICKUP)
                .isPresent() ? 1 : 0;

        for (int i = 0; i < active.size(); i++) {
            Reservation r = active.get(i);
            int newPos = i + 1 + offset;
            if (r.getQueuePosition() == null || r.getQueuePosition() != newPos) {
                r.setQueuePosition(newPos);
                reservationRepository.save(r);
            }
        }
    }

    private BookDTO fetchBook(Long bookId) {
        try {
            ResponseEntity<ApiResponse<BookDTO>> response = restTemplate.exchange(
                    "http://CATALOG-SERVICE/api/user/books/" + bookId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new EntityNotFoundException("Book " + bookId + " not found.");
            }
            return response.getBody().getData();
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate book with catalog-service: " + e.getMessage());
        }
    }

    private String safeFetchBookTitle(Long bookId) {
        try {
            return fetchBook(bookId).getTitle();
        } catch (Exception e) {
            return "Book #" + bookId;
        }
    }

    private void sendReservationNotification(Long memberId, String bookTitle) {
        try {
            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/reservation",
                    Map.of("memberId", memberId, "bookTitle", bookTitle),
                    Object.class
            );
        } catch (Exception e) {
            System.err.println("[ReservationService] Failed to send reservation notification: " + e.getMessage());
        }
    }

    private void sendReservationReadyNotification(Long memberId, String bookTitle) {
        try {
            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/reservation-ready",
                    Map.of("memberId", memberId, "bookTitle", bookTitle, "pickupWindowDays", PICKUP_WINDOW_DAYS),
                    Object.class
            );
        } catch (Exception e) {
            System.err.println("[ReservationService] Failed to send ready notification: " + e.getMessage());
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
                .queuePosition(reservation.getQueuePosition())
                .readyAt(reservation.getReadyAt())
                .build();
    }
}
