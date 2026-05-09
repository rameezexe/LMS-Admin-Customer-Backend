package com.lms.catalog.repository;

import com.lms.catalog.entity.BookCopy;
import com.lms.catalog.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookId(Long bookId);

    List<BookCopy> findByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);
}
