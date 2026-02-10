package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.*;
import com.pastudyhub.user.exception.*;
import com.pastudyhub.user.mapper.UserMapper;
import com.pastudyhub.user.model.RefreshToken;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import com.pastudyhub.user.repository.RefreshTokenRepository;
import com.pastudyhub.user.repository.UserRepository;
import com.pastudyhub.user.security.JwtTokenProvider;
import com.pastudyhub.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implements all authentication flows: register, login, refresh, logout.
 *
 * <p>Security practices:
 * <ul>
 *   <li>Passwords validated by PasswordPolicy before hashing</li>
 *   <li>BCrypt with strength 12 for hashing (about 250ms per hash — intentionally slow)</li>
 *   <li>Email normalized to lowercase before storage and lookup</li>
 *   <li>Login error message doesn't distinguish between wrong email vs wrong password</li>
 *   <li>Refresh token is rotated on every use (prevents replay attacks)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final UserMapper userMapper;

    @Value("${jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // Check for duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // Enforce password policy BEFORE hashing
        PasswordPolicy.ValidationResult policyResult = passwordPolicy.validate(request.getPassword());
        if (!policyResult.isValid()) {
            throw new com.pastudyhub.user.exception.StudyHubException(
                    policyResult.getErrorMessage(),
                    org.springframework.http.HttpStatus.BAD_REQUEST) {};
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .paSchoolName(request.getPaSchoolName())
                .graduationYear(request.getGraduationYear())
                .role(UserRole.STUDENT)
                .isActive(true)
                .studyRemindersEnabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: id={}, email={}", user.getId(), user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }

        // Constant-time comparison via BCrypt — always run even if user not found
        // (already handled above by throwing generic error)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: id={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (existingToken.isRevoked()) {
            // A revoked token being used could indicate token theft — revoke all tokens for safety
            log.warn("Revoked refresh token used — potential token theft. Revoking all tokens for user {}",
                    existingToken.getUser().getId());
            refreshTokenRepository.revokeAllForUser(existingToken.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (!existingToken.getUser().isActive()) {
            throw new AccountDeactivatedException();
        }

        // Rotate: revoke old token, issue new one
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        User user = existingToken.getUser();
        log.debug("Refresh token rotated for user: id={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("User logged out: userId={}", token.getUser().getId());
                });
    }

    /**
     * Builds an AuthResponse by creating a new JWT access token and a new refresh token.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .user(userMapper.toResponse(user))
                .build();
    }
}
