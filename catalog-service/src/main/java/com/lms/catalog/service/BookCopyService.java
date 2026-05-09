package com.lms.catalog.service;

import com.lms.catalog.dto.BookCopyDTO;
import com.lms.catalog.entity.Book;
import com.lms.catalog.entity.BookCopy;
import com.lms.catalog.entity.CopyStatus;
import com.lms.catalog.repository.BookCopyRepository;
import com.lms.catalog.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    public BookCopyService(BookCopyRepository bookCopyRepository, BookRepository bookRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookCopyDTO addCopy(Long bookId, BookCopyDTO dto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        BookCopy copy = BookCopy.builder()
                .bookId(bookId)
                .copyNumber(dto.getCopyNumber())
                .condition(dto.getCondition() != null ? dto.getCondition() : "Good")
                .status(CopyStatus.AVAILABLE)
                .build();

        BookCopy saved = bookCopyRepository.save(copy);

        book.setTotalCopies(book.getTotalCopies() + 1);
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        return toDTO(saved);
    }

    @Transactional
    public BookCopyDTO updateCopyStatus(Long copyId, CopyStatus newStatus) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new EntityNotFoundException("Book copy not found with id: " + copyId));

        Book book = bookRepository.findById(copy.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + copy.getBookId()));

        CopyStatus oldStatus = copy.getStatus();

        // Adjust availableCopies based on status transition
        if (oldStatus == CopyStatus.AVAILABLE && newStatus != CopyStatus.AVAILABLE) {
            // Was available, now not → decrement
            book.setAvailableCopies(Math.max(0, book.getAvailableCopies() - 1));
        } else if (oldStatus != CopyStatus.AVAILABLE && newStatus == CopyStatus.AVAILABLE) {
            // Was not available, now available → increment
            book.setAvailableCopies(book.getAvailableCopies() + 1);
        }

        copy.setStatus(newStatus);
        bookCopyRepository.save(copy);
        bookRepository.save(book);

        return toDTO(copy);
    }

    @Transactional
    public void deleteCopy(Long copyId) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new EntityNotFoundException("Book copy not found with id: " + copyId));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot delete copy with status: " + copy.getStatus() +
                    ". Only AVAILABLE copies can be deleted.");
        }

        Book book = bookRepository.findById(copy.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + copy.getBookId()));

        book.setTotalCopies(Math.max(0, book.getTotalCopies() - 1));
        book.setAvailableCopies(Math.max(0, book.getAvailableCopies() - 1));
        bookRepository.save(book);

        bookCopyRepository.delete(copy);
    }

    public List<BookCopyDTO> getCopiesByBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new EntityNotFoundException("Book not found with id: " + bookId);
        }
        return bookCopyRepository.findByBookId(bookId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private BookCopyDTO toDTO(BookCopy copy) {
        return BookCopyDTO.builder()
                .id(copy.getId())
                .bookId(copy.getBookId())
                .copyNumber(copy.getCopyNumber())
                .condition(copy.getCondition())
                .status(copy.getStatus())
                .build();
    }
}
