package com.glb.campulsebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Partial update (PATCH): null fields are left unchanged; an empty interests list clears them. */
public record UpdateProfileRequest(
		@Size(max = 100, message = "Name must be at most 100 characters") String fullName,
		@Size(max = 100, message = "Program must be at most 100 characters") String program,
		@Size(max = 30, message = "Study year must be at most 30 characters") String studyYear,
		@Size(max = 20, message = "At most 20 interests") List<@NotBlank @Size(max = 50) String> interests) {
}
