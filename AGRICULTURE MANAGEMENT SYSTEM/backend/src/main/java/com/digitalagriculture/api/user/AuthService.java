package com.digitalagriculture.api.user;

import com.digitalagriculture.api.web.ApiException;
import com.digitalagriculture.api.web.dto.RegisterRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password must be no more than 72 UTF-8 bytes.");
        }
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        String accountType = request.accountType() == null ? "" : request.accountType().trim();
        AccountRole role = switch (accountType.toLowerCase(java.util.Locale.ROOT)) {
            case "farmer" -> AccountRole.FARMER;
            case "buyer" -> AccountRole.BUYER;
            case "expert" -> AccountRole.EXPERT;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid account type.");
        };
        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone().trim(),
                passwordEncoder.encode(request.password()),
                role);
        return users.save(user);
    }

    @Transactional
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the email linked to your account.");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        Optional<User> account = users.findByEmailIgnoreCase(normalizedEmail);
        if (account.isEmpty()) {
            return null;
        }
        User user = account.get();
        String resetToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));
        user.setPasswordResetToken(resetToken, expiresAt);
        users.save(user);
        return resetToken;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A valid reset token is required.");
        }
        if (newPassword == null || newPassword.length() < 12 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be between 12 and 72 characters and no longer than 72 UTF-8 bytes.");
        }
        User user = users.findByPasswordResetToken(token.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired."));
        Instant expiry = user.getPasswordResetExpiresAt();
        if (expiry == null || expiry.isBefore(Instant.now())) {
            user.clearPasswordResetToken();
            users.save(user);
            throw new ApiException(HttpStatus.BAD_REQUEST, "This reset link has expired. Request a new one.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken();
        users.save(user);
    }
}
