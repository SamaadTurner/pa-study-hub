package com.pastudyhub.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for PUT /api/v1/users/me — all fields are optional. */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 200, message = "PA school name must not exceed 200 characters")
    private String paSchoolName;

    @Min(value = 2020, message = "Graduation year must be 2020 or later")
    @Max(value = 2040, message = "Graduation year must be 2040 or earlier")
    private Integer graduationYear;

    // Fields left out are not changed, so "no year" can't be said with a missing
    // graduationYear. True removes the year.
    private Boolean clearGraduationYear;

    private Boolean studyRemindersEnabled;

    @Size(max = 5, message = "Preferred study time must be in HH:mm format")
    private String preferredStudyTime;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
}
