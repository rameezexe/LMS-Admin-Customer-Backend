package com.lms.catalog.service;

import com.lms.catalog.dto.BookDTO;
import com.lms.catalog.entity.Book;
import com.lms.catalog.entity.BookStatus;
import com.lms.catalog.entity.CopyStatus;
import com.lms.catalog.repository.BookCopyRepository;
import com.lms.catalog.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final S3Service s3Service;

    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository, S3Service s3Service) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.s3Service = s3Service;
    }

    public BookDTO addBook(BookDTO dto) {
        if (bookRepository.existsByIsbn(dto.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN '" + dto.getIsbn() + "' already exists");
        }

        Book book = Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .isbn(dto.getIsbn())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : BookStatus.ACTIVE)
                .build();

        return toDTO(bookRepository.save(book));
    }

    public BookDTO updateBook(Long id, BookDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));

        if (dto.getIsbn() != null && !dto.getIsbn().equals(book.getIsbn())) {
            if (bookRepository.existsByIsbn(dto.getIsbn())) {
                throw new IllegalArgumentException("Book with ISBN '" + dto.getIsbn() + "' already exists");
            }
        }

        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setCategory(dto.getCategory());
        book.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            book.setStatus(dto.getStatus());
        }

        return toDTO(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));

        long borrowedCount = bookCopyRepository.countByBookIdAndStatus(id, CopyStatus.BORROWED);
        long reservedCount = bookCopyRepository.countByBookIdAndStatus(id, CopyStatus.RESERVED);

        if (borrowedCount > 0 || reservedCount > 0) {
            throw new IllegalStateException("Cannot delete book with active issues (" +
                    borrowedCount + " borrowed, " + reservedCount + " reserved)");
        }

        bookCopyRepository.findByBookId(id).forEach(copy -> bookCopyRepository.delete(copy));
        bookRepository.delete(book);
    }

    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));
        return toDTO(book);
    }

    public BookDTO uploadBookCover(Long bookId, org.springframework.web.multipart.MultipartFile file) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
        
        if (book.getCoverImageUrl() != null) {
            s3Service.deleteFile(book.getCoverImageUrl());
        }
        
        String url = s3Service.uploadFile(file, "book-covers");
        book.setCoverImageUrl(url);
        return toDTO(bookRepository.save(book));
    }

    @Transactional
    public void removeBookCover(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
        
        if (book.getCoverImageUrl() != null) {
            s3Service.deleteFile(book.getCoverImageUrl());
            book.setCoverImageUrl(null);
            bookRepository.save(book);
        }
    }

    public Page<BookDTO> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<BookDTO> searchBooks(String title, String author, String category, Pageable pageable) {
        title = title == null ? "" : title;
        author = author == null ? "" : author;
        category = category == null ? "" : category;
        return bookRepository.searchBooks(title, author, category, pageable).map(this::toDTO);
    }

    public List<BookDTO> getAvailableBooks() {
        return bookRepository.findByAvailableCopiesGreaterThan(0).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<String> getDistinctCategories() {
        return bookRepository.findDistinctCategories();
    }

    private BookDTO toDTO(Book book) {
        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .description(book.getDescription())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .status(book.getStatus())
                .coverImageUrl(book.getCoverImageUrl())
                .build();
    }
}
