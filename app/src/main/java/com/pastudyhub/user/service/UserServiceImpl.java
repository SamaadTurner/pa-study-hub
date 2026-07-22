package com.pastudyhub.user.service;

import com.pastudyhub.user.dto.ChangePasswordRequest;
import com.pastudyhub.user.dto.UpdateProfileRequest;
import com.pastudyhub.user.dto.UserResponse;
import com.pastudyhub.user.exception.InvalidCredentialsException;
import com.pastudyhub.user.exception.UserNotFoundException;
import com.pastudyhub.user.mapper.UserMapper;
import com.pastudyhub.user.model.User;
import com.pastudyhub.user.repository.UserRepository;
import com.pastudyhub.user.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final UserMapper userMapper;

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

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Validate new password meets policy
        PasswordPolicy.ValidationResult policyResult = passwordPolicy.validate(request.getNewPassword());
        if (!policyResult.isValid()) {
            throw new com.pastudyhub.user.exception.StudyHubException(
                    policyResult.getErrorMessage(), HttpStatus.BAD_REQUEST) {};
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: id={}", userId);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated for user: id={}", userId);
    }
}
