package com.lms.auth;

import com.lms.auth.entity.Role;
import com.lms.auth.entity.UserAccount;
import com.lms.auth.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.count() == 0) {
            UserAccount admin = UserAccount.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .memberId(null)
                    .active(true)
                    .build();
            userAccountRepository.save(admin);

            UserAccount user1 = UserAccount.builder()
                    .username("user1")
                    .passwordHash(passwordEncoder.encode("User1@123"))
                    .role(Role.ROLE_USER)
                    .memberId(1L)
                    .active(true)
                    .build();
            userAccountRepository.save(user1);

            UserAccount user2 = UserAccount.builder()
                    .username("user2")
                    .passwordHash(passwordEncoder.encode("User2@123"))
                    .role(Role.ROLE_USER)
                    .memberId(2L)
                    .active(true)
                    .build();
            userAccountRepository.save(user2);

            System.out.println("=== Auth DataLoader: Seeded 3 accounts (admin, user1, user2) ===");
        }
    }
}
