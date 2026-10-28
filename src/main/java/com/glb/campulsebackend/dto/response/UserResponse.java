package com.glb.campulsebackend.dto.response;

import java.util.List;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String role,
		String program,
		String studyYear,
		List<String> interests) {
}
