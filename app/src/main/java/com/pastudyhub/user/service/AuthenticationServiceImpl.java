package com.pastudyhub.user.service;

import com.pastudyhub.user.botcheck.BotCheck;
import com.pastudyhub.user.dto.*;
import com.pastudyhub.user.exception.*;
import com.pastudyhub.user.google.GoogleIdentity;
import com.pastudyhub.user.google.GoogleTokenVerifier;
import com.pastudyhub.user.mapper.UserMapper;
import com.pastudyhub.user.model.EmailToken;
import com.pastudyhub.user.model.RefreshToken;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.model.UserRole;
import com.pastudyhub.user.repository.RefreshTokenRepository;
import com.pastudyhub.user.repository.UserRepository;
import com.pastudyhub.user.security.JwtTokenProvider;
import com.pastudyhub.user.security.LoginAttemptLimiter;
import com.pastudyhub.user.security.SignupLimiter;
import com.pastudyhub.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailLinkService emailLinkService;
    private final SignupLimiter signupLimiter;
    private final BotCheck botCheck;

    @Value("${jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    // Empty means Google sign-in is off
    @Value("${google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // First, so a script learns nothing (not even whether an email is taken)
        // without getting past the check
        botCheck.verify(request.getBotCheckToken());
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

        signupLimiter.checkRoomForOneMore();

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

        // Checked first, so a locked email gets no answer about its password
        loginAttemptLimiter.checkAllowed(normalizedEmail);

        // Unknown emails count as failures too. Otherwise the lock would only
        // happen for emails that have an account, which tells an attacker which do.
        // An account made with Google has no password, so it never matches here
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(normalizedEmail);
            throw new InvalidCredentialsException();
        }

        // Only after the password is right, so a wrong password never reveals
        // that an account is deactivated
        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }

        loginAttemptLimiter.recordSuccess(normalizedEmail);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: id={}", user.getId());
        return buildAuthResponse(user);
    }

    // noRollbackFor: when a revoked token is used again, this method revokes every
    // token of the user and then throws. A normal @Transactional would undo that
    // revoke because of the exception, and the stolen-token check would do nothing.
    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
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

    @Override
    @Transactional
    public AuthResponse resetPassword(NewPasswordRequest request) {
        // The password rules first, so a weak password doesn't use up the link
        PasswordPolicy.ValidationResult policyResult = passwordPolicy.validate(request.getNewPassword());
        if (!policyResult.isValid()) {
            throw new com.pastudyhub.user.exception.StudyHubException(
                    policyResult.getErrorMessage(), org.springframework.http.HttpStatus.BAD_REQUEST) {};
        }
        User user = emailLinkService.useLink(request.getToken(), EmailToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> new InvalidEmailLinkException(
                        "This reset link is invalid, used or expired. Ask for a new one."));
        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        // Whoever knew the old password is signed out everywhere
        refreshTokenRepository.deleteAllForUser(user.getId());
        loginAttemptLimiter.recordSuccess(user.getEmail());
        log.info("Password reset by email: id={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void confirmEmail(EmailLinkRequest request) {
        User user = emailLinkService.useLink(request.getToken(), EmailToken.Purpose.CONFIRM_EMAIL)
                .orElseThrow(() -> new InvalidEmailLinkException(
                        "This confirmation link is invalid, used or expired. Send a new one from the app."));
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email confirmed: id={}", user.getId());
    }

    @Override
    @Transactional
    public AuthResponse issueTokensFor(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return buildAuthResponse(user);
    }

    @Override
    public GoogleStatusResponse googleStatus() {
        boolean enabled = !googleClientId.isBlank();
        return new GoogleStatusResponse(enabled, enabled ? googleClientId : null);
    }

    @Override
    @Transactional
    public AuthResponse signInWithGoogle(GoogleSignInRequest request) {
        GoogleIdentity google = verifyGoogleToken(request.getCredential());

        // Already linked: the Google id is what counts, even if the email changed since
        User linked = userRepository.findByGoogleSubject(google.subject()).orElse(null);
        if (linked != null) {
            return finishGoogleSignIn(linked);
        }

        // Same email as an account that exists. If that account confirmed its email,
        // the owner of the inbox made it, and Google has just shown that this person
        // owns the inbox too, so the two are linked. An unconfirmed account could have
        // been made by someone else with this address (account pre-hijacking), so it
        // is only linked with that account's password (linkGoogle).
        User sameEmail = userRepository.findByEmail(google.email()).orElse(null);
        if (sameEmail != null) {
            if (!sameEmail.isEmailVerified() || sameEmail.getGoogleSubject() != null) {
                throw new GoogleLinkRequiredException();
            }
            sameEmail.setGoogleSubject(google.subject());
            log.info("Google sign-in linked to a confirmed email: id={}", sameEmail.getId());
            return finishGoogleSignIn(sameEmail);
        }

        signupLimiter.checkRoomForOneMore();
        User user = User.builder()
                .email(google.email())
                .passwordHash(null)
                .googleSubject(google.subject())
                .emailVerified(true)
                .firstName(nameOrDefault(google.firstName(), "Student"))
                .lastName(nameOrDefault(google.lastName(), ""))
                .role(UserRole.STUDENT)
                .build();
        User saved = userRepository.save(user);
        log.info("Account made with Google: id={}", saved.getId());
        return finishGoogleSignIn(saved);
    }

    @Override
    @Transactional
    public AuthResponse linkGoogle(GoogleLinkRequest request) {
        GoogleIdentity google = verifyGoogleToken(request.getCredential());

        // The password guess counts toward the same lock as a normal login
        loginAttemptLimiter.checkAllowed(google.email());
        User user = userRepository.findByEmail(google.email()).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(google.email());
            throw new InvalidCredentialsException();
        }
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(google.subject())) {
            throw new GoogleAccountMismatchException();
        }
        loginAttemptLimiter.recordSuccess(google.email());

        user.setGoogleSubject(google.subject());
        // Google has checked the address, so it counts as confirmed now
        user.setEmailVerified(true);
        log.info("Google sign-in linked: id={}", user.getId());
        return finishGoogleSignIn(user);
    }

    private GoogleIdentity verifyGoogleToken(String credential) {
        if (googleClientId.isBlank()) {
            throw new GoogleSignInDisabledException();
        }
        return googleTokenVerifier.verify(credential);
    }

    private AuthResponse finishGoogleSignIn(User user) {
        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    // Names from Google can be missing or longer than the 50 characters the table allows
    private String nameOrDefault(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String trimmed = name.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
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
