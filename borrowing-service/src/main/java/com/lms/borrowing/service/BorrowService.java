package com.lms.borrowing.service;

import com.lms.borrowing.dto.ApiResponse;
import com.lms.borrowing.dto.BorrowRequestDTO;
import com.lms.borrowing.dto.BorrowResponseDTO;
import com.lms.borrowing.dto.MemberDTO;
import com.lms.borrowing.entity.BorrowRecord;
import com.lms.borrowing.entity.BorrowStatus;
import com.lms.borrowing.repository.BorrowRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final RestTemplate restTemplate;

    public BorrowService(BorrowRecordRepository borrowRecordRepository, RestTemplate restTemplate) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.restTemplate = restTemplate;
    }

    public BorrowResponseDTO issueBook(BorrowRequestDTO request) {
        // RULE 1: Max 5 active borrows
        long activeCount = borrowRecordRepository.countByMemberIdAndStatus(request.getMemberId(), BorrowStatus.ACTIVE);
        if (activeCount >= 5) {
            throw new IllegalStateException("Borrow limit reached (max 5 active borrows allowed).");
        }

        // RULE 2: Copy not already issued
        borrowRecordRepository.findByBookCopyIdAndStatus(request.getBookCopyId(), BorrowStatus.ACTIVE)
                .ifPresent(b -> {
                    throw new IllegalStateException("This book copy is already issued and active.");
                });

        // RULE 3: Member status must be ACTIVE
        try {
            ResponseEntity<ApiResponse<MemberDTO>> response = restTemplate.exchange(
                    "http://MEMBER-SERVICE/api/admin/members/" + request.getMemberId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new EntityNotFoundException("Member not found in member-service.");
            }

            MemberDTO member = response.getBody().getData();
            if (!"ACTIVE".equals(member.getStatus())) {
                throw new IllegalStateException("Member status is not ACTIVE. Cannot issue book.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify member status: " + e.getMessage());
        }

        BorrowRecord record = BorrowRecord.builder()
                .memberId(request.getMemberId())
                .bookCopyId(request.getBookCopyId())
                .bookId(request.getBookId())
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .status(BorrowStatus.ACTIVE)
                .issuedByLibrarianId(request.getLibrarianId())
                .build();

        return toDTO(borrowRecordRepository.save(record));
    }

    public BorrowResponseDTO returnBook(Long recordId, Long requestMemberId, boolean isAdmin) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Borrow record not found."));

        if (!isAdmin && !record.getMemberId().equals(requestMemberId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only return your own books.");
        }

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException("This book has already been returned.");
        }

        record.setReturnDate(LocalDate.now());
        record.setStatus(BorrowStatus.RETURNED);

        if (LocalDate.now().isAfter(record.getDueDate())) {
            long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
            record.setFineAmount(BigDecimal.valueOf(daysOverdue * 5));
        }

        return toDTO(borrowRecordRepository.save(record));
    }

    public List<BorrowResponseDTO> getActiveBorrows(Long memberId) {
        return borrowRecordRepository.findByMemberIdAndStatus(memberId, BorrowStatus.ACTIVE)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BorrowResponseDTO> getBorrowHistory(Long memberId) {
        List<BorrowRecord> records = borrowRecordRepository.findByMemberId(memberId);
        records.sort((a, b) -> b.getIssueDate().compareTo(a.getIssueDate()));
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BorrowResponseDTO> getAllOverdue() {
        return borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Page<BorrowResponseDTO> adminGetAllBorrows(Pageable pageable) {
        return borrowRecordRepository.findAll(pageable).map(this::toDTO);
    }

    public BorrowResponseDTO waiveFine(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Borrow record not found."));
        record.setFineAmount(BigDecimal.ZERO);
        record.setFinePaid(true);
        return toDTO(borrowRecordRepository.save(record));
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdue() {
        List<BorrowRecord> activeRecords = borrowRecordRepository.findByDueDateBeforeAndStatus(LocalDate.now(), BorrowStatus.ACTIVE);
        for (BorrowRecord record : activeRecords) {
            record.setStatus(BorrowStatus.OVERDUE);
            long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
            record.setFineAmount(BigDecimal.valueOf(daysOverdue * 5));
            borrowRecordRepository.save(record);
        }
    }

    public long getTotalActive() {
        return borrowRecordRepository.findByStatus(BorrowStatus.ACTIVE).size();
    }

    public long getTotalOverdue() {
        return borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE).size();
    }

    public long getTotalReturned() {
        return borrowRecordRepository.findByStatus(BorrowStatus.RETURNED).size();
    }

    public long getTotalFinesUnpaid() {
        return borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE).stream()
                .filter(b -> !b.isFinePaid())
                .count();
    }

    private BorrowResponseDTO toDTO(BorrowRecord record) {
        return BorrowResponseDTO.builder()
                .id(record.getId())
                .memberId(record.getMemberId())
                .bookCopyId(record.getBookCopyId())
                .bookId(record.getBookId())
                .issueDate(record.getIssueDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .status(record.getStatus())
                .fineAmount(record.getFineAmount())
                .finePaid(record.isFinePaid())
                .issuedByLibrarianId(record.getIssuedByLibrarianId())
                .build();
    }
}
