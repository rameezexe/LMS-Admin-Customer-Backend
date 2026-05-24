package com.lms.auth.service;

import com.lms.auth.dto.*;
import com.lms.auth.entity.OtpPurpose;
import com.lms.auth.entity.OtpToken;
import com.lms.auth.entity.PendingRegistration;
import com.lms.auth.entity.RefreshToken;
import com.lms.auth.entity.Role;
import com.lms.auth.entity.UserAccount;
import com.lms.auth.repository.OtpTokenRepository;
import com.lms.auth.repository.PendingRegistrationRepository;
import com.lms.auth.repository.RefreshTokenRepository;
import com.lms.auth.repository.UserAccountRepository;
import com.lms.auth.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int REGISTRATION_VERIFICATION_WINDOW_MIN = 30;
    private static final int PENDING_REGISTRATION_TTL_MIN = 30;
    private static final int OTP_TTL_MIN = 10;

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.admin.secret}")
    private String adminSecret;

    public AuthService(UserAccountRepository userAccountRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       OtpTokenRepository otpTokenRepository,
                       PendingRegistrationRepository pendingRegistrationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RestTemplate restTemplate) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    @Transactional
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

        // For USER role: auto-create a member in member-service
        Long memberId = request.getMemberId();
        if (role == Role.ROLE_USER && memberId == null && request.getEmail() != null) {
            try {
                Map<String, Object> memberRequest = new HashMap<>();
                memberRequest.put("name", request.getName());
                memberRequest.put("email", request.getEmail());
                memberRequest.put("membershipType", request.getMembershipType());
                memberRequest.put("governmentIdUrl", request.getGovernmentIdUrl());
                memberRequest.put("membershipDuration", request.getMembershipDuration());
                memberRequest.put("membershipAmount", request.getMembershipAmount());
                memberRequest.put("membershipPaymentId", request.getMembershipPaymentId());

                ResponseEntity<Map> memberResponse = restTemplate.postForEntity(
                        "http://MEMBER-SERVICE/api/internal/members",
                        memberRequest,
                        Map.class
                );

                Map body = memberResponse.getBody();
                if (body != null && body.get("data") != null) {
                    Map data = (Map) body.get("data");
                    memberId = ((Number) data.get("id")).longValue();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create member profile: " + e.getMessage());
            }
        }

        UserAccount account = UserAccount.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .memberId(memberId)
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

        // Check member status for USER role (block PENDING_APPROVAL and DECLINED)
        if (account.getRole() == Role.ROLE_USER && account.getMemberId() != null) {
            try {
                String url = "http://MEMBER-SERVICE/api/admin/members/" + account.getMemberId();
                ResponseEntity<Map> memberResponse = restTemplate.getForEntity(url, Map.class);
                Map body = memberResponse.getBody();
                if (body != null && body.get("data") != null) {
                    Map data = (Map) body.get("data");
                    String status = (String) data.get("status");
                    if ("PENDING_APPROVAL".equals(status)) {
                        throw new IllegalArgumentException("Your account is pending admin approval. Please wait for approval.");
                    }
                    if ("DECLINED".equals(status)) {
                        throw new IllegalArgumentException("Your membership application was declined. Your payment will be refunded.");
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                // If member-service is down, allow login (fail-open).
                // NOTE: this is a security trade-off — a downed member-service lets
                // PENDING/DECLINED users in. Consider fail-closed for production.
                System.err.println("[AuthService] Could not check member status: " + e.getMessage());
            }
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

    // ═══════════════════════════════════════════════════════════════════════════
    // FORGOT PASSWORD / RESET PASSWORD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        String email = request.getEmail();

        // Step 1: Call member-service to find memberId by email
        Long memberId;
        try {
            ResponseEntity<Map> memberResponse = restTemplate.getForEntity(
                    "http://MEMBER-SERVICE/api/internal/members/by-email?email=" + email,
                    Map.class
            );

            Map body = memberResponse.getBody();
            if (body == null || body.get("data") == null) {
                throw new EntityNotFoundException("No member found with this email");
            }

            Map data = (Map) body.get("data");
            memberId = ((Number) data.get("id")).longValue();
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify email: " + e.getMessage());
        }

        // Step 2: Find UserAccount by memberId
        UserAccount account = userAccountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("No user account linked to this email"));

        // Step 3: Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Step 4: Save OTP to database
        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        otpTokenRepository.save(otpToken);

        // Step 5: Call notification-service to send OTP email
        try {
            Map<String, String> otpRequest = new HashMap<>();
            otpRequest.put("email", email);
            otpRequest.put("otp", otp);

            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/otp",
                    otpRequest,
                    Object.class
            );
        } catch (Exception e) {
            System.err.println("Failed to send OTP email (OTP saved in DB): " + e.getMessage());
            System.out.println("═══ FALLBACK OTP for " + email + ": " + otp + " ═══");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        // Step 1: Find the latest unused OTP for this email
        OtpToken otpToken = otpTokenRepository.findTopByEmailAndUsedFalseOrderByExpiryTimeDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this email. Please request a new one."));

        // Step 2: Validate OTP
        if (!otpToken.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (otpToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        // Step 3: Mark OTP as used
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        // Step 4: Find memberId from member-service by email
        Long memberId;
        try {
            ResponseEntity<Map> memberResponse = restTemplate.getForEntity(
                    "http://MEMBER-SERVICE/api/internal/members/by-email?email=" + email,
                    Map.class
            );

            Map body = memberResponse.getBody();
            if (body == null || body.get("data") == null) {
                throw new EntityNotFoundException("No member found with this email");
            }

            Map data = (Map) body.get("data");
            memberId = ((Number) data.get("id")).longValue();
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify email: " + e.getMessage());
        }

        // Step 5: Find UserAccount and update password
        UserAccount account = userAccountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("No user account linked to this email"));

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(account);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL-OTP REGISTRATION (Step 1 of the public signup flow)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Send a 6-digit OTP for email-ownership verification. */
    @Transactional
    public void sendRegistrationOtp(String email) {
        // Reject early if the email is already linked to an existing member —
        // saves the user from completing the whole flow only to fail at the end.
        if (isEmailRegistered(email)) {
            throw new IllegalArgumentException("This email is already registered. Please log in instead.");
        }

        String otp = String.format("%06d", new Random().nextInt(999_999));

        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_TTL_MIN))
                .used(false)
                .purpose(OtpPurpose.REGISTRATION)
                .build();
        otpTokenRepository.save(token);

        try {
            Map<String, String> otpRequest = new HashMap<>();
            otpRequest.put("email", email);
            otpRequest.put("otp", otp);

            restTemplate.postForEntity(
                    "http://NOTIFICATION-REPORT-SERVICE/api/internal/notifications/send/otp",
                    otpRequest,
                    Object.class
            );
        } catch (Exception e) {
            // Dev fallback only — same behaviour as forgot-password.
            System.err.println("[AuthService] Failed to email registration OTP: " + e.getMessage());
            System.out.println("═══ FALLBACK REGISTRATION OTP for " + email + ": " + otp + " ═══");
        }
    }

    /**
     * Verify the OTP. On success the row is marked used and assigned a UUID
     * verificationToken that the client passes to registration/initiate to
     * prove they own the email without re-entering the OTP.
     */
    @Transactional
    public EmailVerificationResponseDTO verifyRegistrationOtp(String email, String otp) {
        OtpToken token = otpTokenRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByExpiryTimeDesc(email, OtpPurpose.REGISTRATION)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found. Please request a new one."));

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired. Please request a new one.");
        }

        if (!token.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP.");
        }

        String verificationToken = UUID.randomUUID().toString();
        token.setUsed(true);
        token.setVerifiedAt(LocalDateTime.now());
        token.setVerificationToken(verificationToken);
        // Extend expiry so initiate has time to land within the verification window.
        token.setExpiryTime(LocalDateTime.now().plusMinutes(REGISTRATION_VERIFICATION_WINDOW_MIN));
        otpTokenRepository.save(token);

        return EmailVerificationResponseDTO.builder()
                .verificationToken(verificationToken)
                .build();
    }

    /** Cheap availability check for live UX feedback on Step 2. */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.length() < 3) return false;
        return !userAccountRepository.existsByUsername(username);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRE-PAY REGISTRATION (Steps 5a / 5b — initiate creates the order,
    // complete creates the actual UserAccount + Member after Razorpay succeeds)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public RegistrationInitiateResponseDTO initiateRegistration(RegistrationInitiateRequestDTO request) {
        OtpToken verifiedOtp = otpTokenRepository
                .findByEmailAndVerificationTokenAndPurpose(
                        request.getEmail(), request.getVerificationToken(), OtpPurpose.REGISTRATION)
                .orElseThrow(() -> new IllegalArgumentException("Email verification not found or invalid."));

        if (verifiedOtp.getVerifiedAt() == null
                || verifiedOtp.getVerifiedAt().plusMinutes(REGISTRATION_VERIFICATION_WINDOW_MIN).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Email verification expired. Please re-verify your email.");
        }

        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken.");
        }

        if (isEmailRegistered(request.getEmail())) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        // A previous abandoned attempt for this email is replaced — only the
        // latest order should be promotable to a real account.
        pendingRegistrationRepository.findByEmail(request.getEmail())
                .ifPresent(pendingRegistrationRepository::delete);

        String pendingId = UUID.randomUUID().toString();
        PendingRegistration pending = PendingRegistration.builder()
                .pendingId(pendingId)
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .membershipType(request.getMembershipType())
                .governmentIdUrl(request.getGovernmentIdUrl())
                .membershipDuration(request.getMembershipDuration())
                .membershipAmount(request.getMembershipAmount())
                .expiresAt(LocalDateTime.now().plusMinutes(PENDING_REGISTRATION_TTL_MIN))
                .build();
        pendingRegistrationRepository.save(pending);

        Map<String, Object> orderInfo = createRazorpayMembershipOrder(request.getMembershipAmount());
        String orderId = (String) orderInfo.get("orderId");
        String keyId = (String) orderInfo.get("keyId");
        String currency = (String) orderInfo.getOrDefault("currency", "INR");

        pending.setRazorpayOrderId(orderId);
        pendingRegistrationRepository.save(pending);

        return RegistrationInitiateResponseDTO.builder()
                .pendingId(pendingId)
                .orderId(orderId)
                .keyId(keyId)
                .amount(request.getMembershipAmount())
                .currency(currency)
                .build();
    }

    @Transactional
    public UserAccountResponseDTO completeRegistration(RegistrationCompleteRequestDTO request) {
        PendingRegistration pending = pendingRegistrationRepository.findByPendingId(request.getPendingId())
                .orElseThrow(() -> new IllegalArgumentException("Registration session not found or already completed."));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalArgumentException("Registration session expired. Please start over.");
        }

        if (!request.getRazorpayOrderId().equals(pending.getRazorpayOrderId())) {
            throw new IllegalArgumentException("Order ID mismatch — refusing to bind payment to this registration.");
        }

        // 1. Verify payment with Razorpay (via payment-service)
        verifyRazorpayPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        // 2. Create the Member row (status PENDING_APPROVAL on member-service)
        Long memberId = createMemberRecord(pending, request.getRazorpayPaymentId());

        // 3. Now (and only now) create the UserAccount
        UserAccount account = UserAccount.builder()
                .username(pending.getUsername())
                .passwordHash(pending.getPasswordHash())
                .role(Role.ROLE_USER)
                .memberId(memberId)
                .active(true)
                .build();
        UserAccount saved = userAccountRepository.save(account);

        // 4. Attach the new memberId to the Payment row so reporting/refunds work
        attachMemberIdToPayment(request.getRazorpayOrderId(), memberId);

        // 5. Clean up — pending row and any consumed verification OTPs
        pendingRegistrationRepository.delete(pending);
        otpTokenRepository.deleteAll(
                otpTokenRepository.findAll().stream()
                        .filter(t -> pending.getEmail().equals(t.getEmail()) && t.getPurpose() == OtpPurpose.REGISTRATION)
                        .toList()
        );

        return toResponseDTO(saved);
    }

    /**
     * Daily sweep: any PendingRegistration past its expiresAt is dead weight
     * (the user abandoned signup mid-payment). The Razorpay order will just
     * time out on its own.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredPendingRegistrations() {
        List<PendingRegistration> expired = pendingRegistrationRepository
                .findByExpiresAtBefore(LocalDateTime.now());
        if (!expired.isEmpty()) {
            pendingRegistrationRepository.deleteAll(expired);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private boolean isEmailRegistered(String email) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "http://MEMBER-SERVICE/api/internal/members/by-email?email=" + email,
                    Map.class
            );
            Map body = response.getBody();
            return body != null && body.get("data") != null;
        } catch (Exception e) {
            // If member-service is unreachable we err on the side of letting
            // the user proceed — the initiate step will recheck.
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createRazorpayMembershipOrder(BigDecimal amount) {
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("memberId", null);
        orderRequest.put("amount", amount);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://PAYMENT-SERVICE/api/internal/payments/membership/create-order",
                    orderRequest,
                    Map.class
            );
            Map body = response.getBody();
            if (body == null || body.get("data") == null) {
                throw new IllegalStateException("Empty response from payment-service");
            }
            return (Map<String, Object>) body.get("data");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create payment order: " + e.getMessage());
        }
    }

    private void verifyRazorpayPayment(String orderId, String paymentId, String signature) {
        Map<String, String> request = new HashMap<>();
        request.put("razorpay_order_id", orderId);
        request.put("razorpay_payment_id", paymentId);
        request.put("razorpay_signature", signature);

        try {
            restTemplate.postForEntity(
                    "http://PAYMENT-SERVICE/api/internal/payments/membership/verify",
                    request,
                    Map.class
            );
        } catch (Exception e) {
            throw new IllegalStateException("Payment verification failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Long createMemberRecord(PendingRegistration pending, String razorpayPaymentId) {
        Map<String, Object> memberRequest = new HashMap<>();
        memberRequest.put("name", pending.getName());
        memberRequest.put("email", pending.getEmail());
        memberRequest.put("membershipType", pending.getMembershipType());
        memberRequest.put("governmentIdUrl", pending.getGovernmentIdUrl());
        memberRequest.put("membershipDuration", pending.getMembershipDuration());
        memberRequest.put("membershipAmount", pending.getMembershipAmount());
        memberRequest.put("membershipPaymentId", razorpayPaymentId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://MEMBER-SERVICE/api/internal/members",
                    memberRequest,
                    Map.class
            );
            Map body = response.getBody();
            if (body == null || body.get("data") == null) {
                throw new IllegalStateException("Empty response from member-service");
            }
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            return ((Number) data.get("id")).longValue();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create member profile: " + e.getMessage());
        }
    }

    private void attachMemberIdToPayment(String razorpayOrderId, Long memberId) {
        Map<String, Object> request = new HashMap<>();
        request.put("razorpayOrderId", razorpayOrderId);
        request.put("memberId", memberId);
        try {
            restTemplate.postForEntity(
                    "http://PAYMENT-SERVICE/api/internal/payments/membership/attach-member",
                    request,
                    Object.class
            );
        } catch (Exception e) {
            // Non-fatal — the user account is already created.
            System.err.println("[AuthService] Failed to attach memberId to payment: " + e.getMessage());
        }
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
