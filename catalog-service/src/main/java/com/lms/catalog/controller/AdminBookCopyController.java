package com.lms.catalog.controller;

import com.lms.catalog.dto.ApiResponse;
import com.lms.catalog.dto.BookCopyDTO;
import com.lms.catalog.dto.UpdateCopyStatusDTO;
import com.lms.catalog.service.BookCopyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/books/{bookId}/copies")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Book Copies", description = "Admin book copy management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookCopyController {

    private final BookCopyService bookCopyService;

    public AdminBookCopyController(BookCopyService bookCopyService) {
        this.bookCopyService = bookCopyService;
    }

    @GetMapping
    @Operation(summary = "Get all copies of a book")
    public ResponseEntity<ApiResponse<List<BookCopyDTO>>> getCopiesByBook(@PathVariable Long bookId) {
        List<BookCopyDTO> copies = bookCopyService.getCopiesByBook(bookId);
        return ResponseEntity.ok(ApiResponse.ok("Copies retrieved successfully", copies));
    }

    @PostMapping
    @Operation(summary = "Add a new copy to a book")
    public ResponseEntity<ApiResponse<BookCopyDTO>> addCopy(@PathVariable Long bookId,
                                                             @Valid @RequestBody BookCopyDTO dto) {
        BookCopyDTO created = bookCopyService.addCopy(bookId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Copy added successfully", created));
    }

    @PutMapping("/{copyId}/status")
    @Operation(summary = "Update copy status")
    public ResponseEntity<ApiResponse<BookCopyDTO>> updateCopyStatus(
            @PathVariable Long bookId,
            @PathVariable Long copyId,
            @Valid @RequestBody UpdateCopyStatusDTO dto) {
        BookCopyDTO updated = bookCopyService.updateCopyStatus(copyId, dto.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Copy status updated successfully", updated));
    }

    @DeleteMapping("/{copyId}")
    @Operation(summary = "Delete a copy (only if AVAILABLE)")
    public ResponseEntity<ApiResponse<Void>> deleteCopy(@PathVariable Long bookId,
                                                         @PathVariable Long copyId) {
        bookCopyService.deleteCopy(copyId);
        return ResponseEntity.ok(ApiResponse.ok("Copy deleted successfully", null));
    }
}
