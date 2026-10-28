package com.glb.campulsebackend.mapper;

import com.glb.campulsebackend.dto.response.UserResponse;
import com.glb.campulsebackend.entity.User;

import java.util.List;

public final class UserMapper {

	private UserMapper() {
	}

	/** Must be called inside a transaction: interests are lazily loaded. */
	public static UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getRole().name(),
				user.getProgram(),
				user.getStudyYear(),
				user.getInterests().stream().sorted().toList());
	}
}
