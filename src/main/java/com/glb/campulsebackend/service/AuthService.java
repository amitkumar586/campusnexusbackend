package com.glb.campulsebackend.service;

import com.glb.campulsebackend.dto.request.LoginRequest;
import com.glb.campulsebackend.dto.request.RefreshTokenRequest;
import com.glb.campulsebackend.dto.request.RegisterRequest;
import com.glb.campulsebackend.dto.response.AuthResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	AuthResponse refresh(RefreshTokenRequest request);

	void logout(RefreshTokenRequest request);
}
