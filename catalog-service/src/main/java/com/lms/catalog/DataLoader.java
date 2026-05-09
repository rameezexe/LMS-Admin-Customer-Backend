package com.lms.catalog;

import com.lms.catalog.entity.*;
import com.lms.catalog.repository.BookCopyRepository;
import com.lms.catalog.repository.BookRepository;
import com.lms.catalog.repository.LibrarianRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LibrarianRepository librarianRepository;

    public DataLoader(BookRepository bookRepository,
                      BookCopyRepository bookCopyRepository,
                      LibrarianRepository librarianRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.librarianRepository = librarianRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            // ── Fiction ──
            Book b1 = bookRepository.save(Book.builder()
                    .title("To Kill a Mockingbird").author("Harper Lee")
                    .isbn("978-0-06-112008-4").category("Fiction")
                    .description("A novel about racial injustice in the Deep South.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            Book b2 = bookRepository.save(Book.builder()
                    .title("1984").author("George Orwell")
                    .isbn("978-0-45-152493-5").category("Fiction")
                    .description("A dystopian social science fiction novel.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            // ── Technology ──
            Book b3 = bookRepository.save(Book.builder()
                    .title("Clean Code").author("Robert C. Martin")
                    .isbn("978-0-13-235088-4").category("Technology")
                    .description("A handbook of agile software craftsmanship.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            Book b4 = bookRepository.save(Book.builder()
                    .title("Design Patterns").author("Gang of Four")
                    .isbn("978-0-20-163361-0").category("Technology")
                    .description("Elements of reusable object-oriented software.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            // ── Science ──
            Book b5 = bookRepository.save(Book.builder()
                    .title("A Brief History of Time").author("Stephen Hawking")
                    .isbn("978-0-55-338016-3").category("Science")
                    .description("A landmark volume in science writing.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            Book b6 = bookRepository.save(Book.builder()
                    .title("The Selfish Gene").author("Richard Dawkins")
                    .isbn("978-0-19-857519-1").category("Science")
                    .description("A book on evolution centred on the gene.")
                    .totalCopies(2).availableCopies(2).status(BookStatus.ACTIVE).build());

            // ── 2 copies each ──
            Book[] books = {b1, b2, b3, b4, b5, b6};
            for (Book book : books) {
                bookCopyRepository.save(BookCopy.builder()
                        .bookId(book.getId()).copyNumber(book.getIsbn() + "-C1")
                        .condition("New").status(CopyStatus.AVAILABLE).build());
                bookCopyRepository.save(BookCopy.builder()
                        .bookId(book.getId()).copyNumber(book.getIsbn() + "-C2")
                        .condition("Good").status(CopyStatus.AVAILABLE).build());
            }

            System.out.println("=== Catalog DataLoader: Seeded 6 books with 2 copies each ===");
        }

        if (librarianRepository.count() == 0) {
            librarianRepository.save(Librarian.builder()
                    .name("Alice Johnson").email("alice@lms.com")
                    .phone("555-0101").employeeId("LIB-001")
                    .department("Fiction").build());

            librarianRepository.save(Librarian.builder()
                    .name("Bob Williams").email("bob@lms.com")
                    .phone("555-0102").employeeId("LIB-002")
                    .department("Technology").build());

            System.out.println("=== Catalog DataLoader: Seeded 2 librarians ===");
        }
    }
}
