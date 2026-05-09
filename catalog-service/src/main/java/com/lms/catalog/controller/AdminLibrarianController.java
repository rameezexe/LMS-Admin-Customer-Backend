package com.lms.catalog.controller;

import com.lms.catalog.dto.ApiResponse;
import com.lms.catalog.dto.LibrarianDTO;
import com.lms.catalog.service.LibrarianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/librarians")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Librarians", description = "Admin librarian management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminLibrarianController {

    private final LibrarianService librarianService;

    public AdminLibrarianController(LibrarianService librarianService) {
        this.librarianService = librarianService;
    }

    @GetMapping
    @Operation(summary = "Get all librarians")
    public ResponseEntity<ApiResponse<List<LibrarianDTO>>> getAllLibrarians() {
        List<LibrarianDTO> librarians = librarianService.getAllLibrarians();
        return ResponseEntity.ok(ApiResponse.ok("Librarians retrieved successfully", librarians));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get librarian by ID")
    public ResponseEntity<ApiResponse<LibrarianDTO>> getLibrarianById(@PathVariable Long id) {
        LibrarianDTO librarian = librarianService.getLibrarianById(id);
        return ResponseEntity.ok(ApiResponse.ok("Librarian retrieved successfully", librarian));
    }

    @PostMapping
    @Operation(summary = "Add a new librarian")
    public ResponseEntity<ApiResponse<LibrarianDTO>> addLibrarian(@Valid @RequestBody LibrarianDTO dto) {
        LibrarianDTO created = librarianService.addLibrarian(dto);
        return ResponseEntity.ok(ApiResponse.ok("Librarian added successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a librarian")
    public ResponseEntity<ApiResponse<LibrarianDTO>> updateLibrarian(@PathVariable Long id,
                                                                      @Valid @RequestBody LibrarianDTO dto) {
        LibrarianDTO updated = librarianService.updateLibrarian(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Librarian updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a librarian")
    public ResponseEntity<ApiResponse<Void>> deleteLibrarian(@PathVariable Long id) {
        librarianService.deleteLibrarian(id);
        return ResponseEntity.ok(ApiResponse.ok("Librarian deleted successfully", null));
    }
}
