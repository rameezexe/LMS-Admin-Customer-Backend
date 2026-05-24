package com.lms.borrowing.repository;

import com.lms.borrowing.entity.BorrowRecord;
import com.lms.borrowing.entity.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByMemberId(Long memberId);
    
    List<BorrowRecord> findByMemberIdAndStatus(Long memberId, BorrowStatus status);

    Optional<BorrowRecord> findByBookCopyIdAndStatus(Long bookCopyId, BorrowStatus status);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    long countByMemberIdAndStatus(Long memberId, BorrowStatus status);

    long countByStatus(BorrowStatus status);

    long countByFinePaidFalseAndFineAmountGreaterThan(BigDecimal amount);

    List<BorrowRecord> findByDueDateBeforeAndStatus(LocalDate date, BorrowStatus status);

    List<BorrowRecord> findByReturnDateIsNullAndStatus(BorrowStatus status);
}
