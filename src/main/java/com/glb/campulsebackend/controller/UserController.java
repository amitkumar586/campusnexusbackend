package com.glb.campulsebackend.controller;

import com.glb.campulsebackend.dto.request.UpdateProfileRequest;
import com.glb.campulsebackend.dto.response.ApiResponse;
import com.glb.campulsebackend.dto.response.UserResponse;
import com.glb.campulsebackend.security.CurrentUser;
import com.glb.campulsebackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok("Profile retrieved", userService.getProfile(CurrentUser.id(jwt)));
	}

	@PatchMapping
	public ApiResponse<UserResponse> update(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ApiResponse.ok("Profile updated", userService.updateProfile(CurrentUser.id(jwt), request));
	}
}
