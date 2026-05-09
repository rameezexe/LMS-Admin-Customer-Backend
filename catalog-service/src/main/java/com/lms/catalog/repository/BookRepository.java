package com.lms.catalog.repository;

import com.lms.catalog.entity.Book;
import com.lms.catalog.entity.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    List<Book> findByAuthorIgnoreCase(String author);

    List<Book> findByCategoryIgnoreCase(String category);

    List<Book> findByStatus(BookStatus status);

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByAvailableCopiesGreaterThan(int count);

    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) AND " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')) AND " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :category, '%'))")
    Page<Book> searchBooks(@Param("title") String title,
                           @Param("author") String author,
                           @Param("category") String category,
                           Pageable pageable);

    @Query("SELECT DISTINCT b.category FROM Book b ORDER BY b.category")
    List<String> findDistinctCategories();

    boolean existsByIsbn(String isbn);
}
