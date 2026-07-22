package com.pastudyhub.user.mapper;

import com.pastudyhub.user.dto.UserResponse;
import com.pastudyhub.user.model.User;
import org.springframework.stereotype.Component;

/**
 * Manually-coded mapper for converting between User entities and DTOs.
 *
 * <p>Manual mapping (no MapStruct) is used to:
 * <ul>
 *   <li>Keep dependencies minimal and the mapping logic explicit</li>
 *   <li>Ensure passwordHash is never accidentally included in responses</li>
 *   <li>Allow future mapping logic (e.g., masked email for display) without a code generator</li>
 * </ul>
 */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a public-safe UserResponse DTO.
     * Never includes passwordHash.
     *
     * @param user the User entity
     * @return UserResponse DTO safe for sending to clients
     */
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .paSchoolName(user.getPaSchoolName())
                .graduationYear(user.getGraduationYear())
                .studyRemindersEnabled(user.isStudyRemindersEnabled())
                .preferredStudyTime(user.getPreferredStudyTime())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
