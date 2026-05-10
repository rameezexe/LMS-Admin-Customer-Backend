package com.lms.catalog.controller;

import com.lms.catalog.dto.ApiResponse;
import com.lms.catalog.dto.BookDTO;
import com.lms.catalog.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Books", description = "Admin book management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Get all books (pageable)")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> getAllBooks(
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        Page<BookDTO> books = bookService.getAllBooks(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Books retrieved successfully", books));
    }

    @PostMapping
    @Operation(summary = "Add a new book")
    public ResponseEntity<ApiResponse<BookDTO>> addBook(@Valid @RequestBody BookDTO bookDTO) {
        BookDTO created = bookService.addBook(bookDTO);
        return ResponseEntity.ok(ApiResponse.ok("Book added successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookDTO>> getBookById(@PathVariable Long id) {
        BookDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.ok("Book retrieved successfully", book));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book")
    public ResponseEntity<ApiResponse<BookDTO>> updateBook(@PathVariable Long id,
                                                            @Valid @RequestBody BookDTO bookDTO) {
        BookDTO updated = bookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(ApiResponse.ok("Book updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.ok("Book deleted successfully", null));
    }

    @PostMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @Operation(summary = "Upload book cover image")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BookDTO>> uploadBookCover(@PathVariable Long id, @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        BookDTO updated = bookService.uploadBookCover(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Cover image uploaded successfully", updated));
    }

    @DeleteMapping("/{id}/cover")
    @Operation(summary = "Remove book cover image")
    public ResponseEntity<ApiResponse<Void>> removeBookCover(@PathVariable Long id) {
        bookService.removeBookCover(id);
        return ResponseEntity.ok(ApiResponse.ok("Cover image removed successfully", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title, author, or category")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        Page<BookDTO> results = bookService.searchBooks(title, author, category, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Search results", results));
    }
}
