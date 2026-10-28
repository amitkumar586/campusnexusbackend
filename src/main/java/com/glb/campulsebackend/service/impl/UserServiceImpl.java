package com.glb.campulsebackend.service.impl;

import com.glb.campulsebackend.dto.request.UpdateProfileRequest;
import com.glb.campulsebackend.dto.response.UserResponse;
import com.glb.campulsebackend.entity.User;
import com.glb.campulsebackend.exception.UnauthorizedException;
import com.glb.campulsebackend.mapper.UserMapper;
import com.glb.campulsebackend.repository.UserRepository;
import com.glb.campulsebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

	private final UserRepository users;

	public UserServiceImpl(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getProfile(Long userId) {
		return UserMapper.toResponse(load(userId));
	}

	@Override
	@Transactional
	public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = load(userId);
		if (request.fullName() != null && !request.fullName().isBlank()) {
			user.setFullName(request.fullName().trim());
		}
		if (request.program() != null) {
			user.setProgram(blankToNull(request.program()));
		}
		if (request.studyYear() != null) {
			user.setStudyYear(blankToNull(request.studyYear()));
		}
		if (request.interests() != null) {
			user.getInterests().clear();
			request.interests().forEach(i -> user.getInterests().add(i.trim().toLowerCase(Locale.ROOT)));
		}
		return UserMapper.toResponse(user);
	}

	/** A valid token for a deleted account must not look like a normal 404. */
	private User load(Long userId) {
		return users.findById(userId).orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
	}

	private static String blankToNull(String value) {
		return value.isBlank() ? null : value.trim();
	}
}
