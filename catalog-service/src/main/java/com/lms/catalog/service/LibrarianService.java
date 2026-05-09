package com.lms.catalog.service;

import com.lms.catalog.dto.LibrarianDTO;
import com.lms.catalog.entity.Librarian;
import com.lms.catalog.repository.LibrarianRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibrarianService {

    private final LibrarianRepository librarianRepository;

    public LibrarianService(LibrarianRepository librarianRepository) {
        this.librarianRepository = librarianRepository;
    }

    public LibrarianDTO addLibrarian(LibrarianDTO dto) {
        if (librarianRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Librarian with email '" + dto.getEmail() + "' already exists");
        }
        if (librarianRepository.existsByEmployeeId(dto.getEmployeeId())) {
            throw new IllegalArgumentException("Librarian with employee ID '" + dto.getEmployeeId() + "' already exists");
        }

        Librarian librarian = Librarian.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .employeeId(dto.getEmployeeId())
                .department(dto.getDepartment())
                .build();

        return toDTO(librarianRepository.save(librarian));
    }

    public LibrarianDTO updateLibrarian(Long id, LibrarianDTO dto) {
        Librarian librarian = librarianRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Librarian not found with id: " + id));

        librarian.setName(dto.getName());
        librarian.setEmail(dto.getEmail());
        librarian.setPhone(dto.getPhone());
        librarian.setEmployeeId(dto.getEmployeeId());
        librarian.setDepartment(dto.getDepartment());

        return toDTO(librarianRepository.save(librarian));
    }

    public void deleteLibrarian(Long id) {
        if (!librarianRepository.existsById(id)) {
            throw new EntityNotFoundException("Librarian not found with id: " + id);
        }
        librarianRepository.deleteById(id);
    }

    public LibrarianDTO getLibrarianById(Long id) {
        Librarian librarian = librarianRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Librarian not found with id: " + id));
        return toDTO(librarian);
    }

    public List<LibrarianDTO> getAllLibrarians() {
        return librarianRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private LibrarianDTO toDTO(Librarian librarian) {
        return LibrarianDTO.builder()
                .id(librarian.getId())
                .name(librarian.getName())
                .email(librarian.getEmail())
                .phone(librarian.getPhone())
                .employeeId(librarian.getEmployeeId())
                .department(librarian.getDepartment())
                .build();
    }
}
