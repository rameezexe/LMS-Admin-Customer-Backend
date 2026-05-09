package com.lms.catalog.controller;

import com.lms.catalog.dto.ApiResponse;
import com.lms.catalog.dto.BookDTO;
import com.lms.catalog.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/books")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "User - Books", description = "User book browsing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserBookController {

    private final BookService bookService;

    public UserBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Get all books (pageable)")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> getAllBooks(
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        Page<BookDTO> books = bookService.getAllBooks(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Books retrieved successfully", books));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookDTO>> getBookById(@PathVariable Long id) {
        BookDTO book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.ok("Book retrieved successfully", book));
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

    @GetMapping("/available")
    @Operation(summary = "Get all available books")
    public ResponseEntity<ApiResponse<List<BookDTO>>> getAvailableBooks() {
        List<BookDTO> books = bookService.getAvailableBooks();
        return ResponseEntity.ok(ApiResponse.ok("Available books retrieved successfully", books));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get distinct list of all categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = bookService.getDistinctCategories();
        return ResponseEntity.ok(ApiResponse.ok("Categories retrieved successfully", categories));
    }
}
