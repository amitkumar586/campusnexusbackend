package com.glb.campulsebackend.service;

import com.glb.campulsebackend.dto.request.UpdateProfileRequest;
import com.glb.campulsebackend.dto.response.UserResponse;

public interface UserService {

	UserResponse getProfile(Long userId);

	UserResponse updateProfile(Long userId, UpdateProfileRequest request);
}
