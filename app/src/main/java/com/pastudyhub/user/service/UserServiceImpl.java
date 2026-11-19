package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.DeleteAccountRequest;
import com.pastudyhub.user.dto.UpdateProfileRequest;
import com.pastudyhub.user.dto.UserResponse;
import com.pastudyhub.user.exception.AccountDeletionNotConfirmedException;
import com.pastudyhub.user.exception.GoogleAccountInUseException;
import com.pastudyhub.user.exception.GoogleAccountMismatchException;
import com.pastudyhub.user.exception.GoogleSignInDisabledException;
import com.pastudyhub.user.exception.GoogleUnlinkNotAllowedException;
import com.pastudyhub.user.exception.WrongCurrentPasswordException;
import com.pastudyhub.user.google.GoogleIdentity;
import com.pastudyhub.user.google.GoogleTokenVerifier;
import com.pastudyhub.user.exception.UserNotFoundException;
import com.pastudyhub.user.mapper.UserMapper;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.RefreshTokenRepository;
import com.pastudyhub.user.repository.UserRepository;
import com.pastudyhub.user.security.LoginAttemptLimiter;
import com.pastudyhub.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of user profile management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final UserMapper userMapper;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AccountDeletionService accountDeletionService;

    // Empty means Google sign-in is off
    @Value("${google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Partial update — only update fields that are non-null in the request
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPaSchoolName() != null) {
            user.setPaSchoolName(request.getPaSchoolName());
        }
        if (request.getGraduationYear() != null) {
            user.setGraduationYear(request.getGraduationYear());
        }
        if (request.getStudyRemindersEnabled() != null) {
            user.setStudyRemindersEnabled(request.getStudyRemindersEnabled());
        }
        if (request.getPreferredStudyTime() != null) {
            user.setPreferredStudyTime(request.getPreferredStudyTime());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User saved = userRepository.save(user);
        log.debug("Profile updated for user: id={}", userId);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // An account made with Google has no password yet and may set one. Any other
        // account must prove it knows the current one. A wrong guess counts toward the
        // login lock, so a stolen session can't be used to try passwords forever.
        if (user.getPasswordHash() != null) {
            loginAttemptLimiter.checkAllowed(user.getEmail());
            String current = request.getCurrentPassword();
            if (current == null || !passwordEncoder.matches(current, user.getPasswordHash())) {
                loginAttemptLimiter.recordFailure(user.getEmail());
                // 400 and not 401: a 401 would make the app think the session expired
                throw new WrongCurrentPasswordException();
            }
        }

        // Validate new password meets policy
        PasswordPolicy.ValidationResult policyResult = passwordPolicy.validate(request.getNewPassword());
        if (!policyResult.isValid()) {
            throw new com.pastudyhub.user.exception.StudyHubException(
                    policyResult.getErrorMessage(), HttpStatus.BAD_REQUEST) {};
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Sign out everywhere. A refresh token from before the change may be in the
        // hands of whoever made the user want a new password.
        refreshTokenRepository.deleteAllForUser(userId);

        log.info("Password changed for user: id={}", userId);
    }

    @Override
    @Transactional
    public UserResponse linkGoogle(UUID userId, String credential) {
        if (googleClientId.isBlank()) {
            throw new GoogleSignInDisabledException();
        }
        User user = findUser(userId);
        GoogleIdentity google = googleTokenVerifier.verify(credential);

        // Being logged in proves this account is yours, and the token proves the
        // Google account is, so no password is needed here. The Google email may
        // differ from the account's email.
        userRepository.findByGoogleSubject(google.subject()).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new GoogleAccountInUseException();
            }
        });
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(google.subject())) {
            throw new GoogleAccountMismatchException();
        }
        user.setGoogleSubject(google.subject());
        log.info("Google sign-in added from the profile: id={}", userId);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse unlinkGoogle(UUID userId) {
        User user = findUser(userId);
        // Without a password, removing Google would leave no way to sign in
        if (user.getPasswordHash() == null) {
            throw new GoogleUnlinkNotAllowedException();
        }
        user.setGoogleSubject(null);
        log.info("Google sign-in removed: id={}", userId);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = findUser(userId);

        // Closing an account can't be undone, so it has to be confirmed: the password,
        // or for an account without one, its email typed out
        if (user.getPasswordHash() != null) {
            loginAttemptLimiter.checkAllowed(user.getEmail());
            String password = request.getPassword();
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                loginAttemptLimiter.recordFailure(user.getEmail());
                throw new AccountDeletionNotConfirmedException();
            }
        } else {
            String typed = request.getConfirmEmail();
            if (typed == null || !typed.trim().equalsIgnoreCase(user.getEmail())) {
                throw new AccountDeletionNotConfirmedException();
            }
        }

        accountDeletionService.deleteEverything(userId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
