package com.lms.auth.service;

import com.lms.auth.dto.*;
import com.lms.auth.entity.RefreshToken;
import com.lms.auth.entity.Role;
import com.lms.auth.entity.UserAccount;
import com.lms.auth.repository.RefreshTokenRepository;
import com.lms.auth.repository.UserAccountRepository;
import com.lms.auth.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.admin.secret}")
    private String adminSecret;

    public AuthService(UserAccountRepository userAccountRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public UserAccountResponseDTO register(RegisterRequestDTO request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }

        Role role = Role.ROLE_USER;
        if ("ROLE_ADMIN".equals(request.getRole())) {
            if (request.getAdminSecret() == null || !request.getAdminSecret().equals(adminSecret)) {
                throw new IllegalArgumentException("Invalid admin secret key");
            }
            role = Role.ROLE_ADMIN;
        }

        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .memberId(request.getMemberId())
                .active(true)
                .build();

        UserAccount saved = userAccountRepository.save(account);
        return toResponseDTO(saved);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        UserAccount account = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!account.isActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        account.setLastLogin(LocalDateTime.now());
        userAccountRepository.save(account);

        String accessToken = jwtUtil.generateToken(account.getUsername(), account.getRole().name(), account.getMemberId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userAccountId(account.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(account.getRole().name())
                .memberId(account.getMemberId())
                .expiresIn(jwtExpiration)
                .build();
    }

    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (existing.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (existing.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        UserAccount account = userAccountRepository.findById(existing.getUserAccountId())
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        String newAccessToken = jwtUtil.generateToken(account.getUsername(), account.getRole().name(), account.getMemberId());

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userAccountId(account.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .role(account.getRole().name())
                .memberId(account.getMemberId())
                .expiresIn(jwtExpiration)
                .build();
    }

    @Transactional
    public void logout(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<RefreshToken> tokens = refreshTokenRepository.findByUserAccountId(account.getId());
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    public void changePassword(ChangePasswordRequestDTO request, String usernameFromJwt) {
        UserAccount account = userAccountRepository.findByUsername(usernameFromJwt)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(account);
    }

    public void deactivateAccount(Long userAccountId) {
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found with id: " + userAccountId));
        account.setActive(false);
        userAccountRepository.save(account);
    }

    public void activateAccount(Long userAccountId) {
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found with id: " + userAccountId));
        account.setActive(true);
        userAccountRepository.save(account);
    }

    public List<UserAccountResponseDTO> getAllUserAccounts() {
        return userAccountRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private UserAccountResponseDTO toResponseDTO(UserAccount account) {
        return UserAccountResponseDTO.builder()
                .id(account.getId())
                .username(account.getUsername())
                .role(account.getRole())
                .memberId(account.getMemberId())
                .active(account.isActive())
                .lastLogin(account.getLastLogin())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
