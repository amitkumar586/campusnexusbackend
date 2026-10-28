package com.glb.campulsebackend.service.impl;

import com.glb.campulsebackend.dto.request.LoginRequest;
import com.glb.campulsebackend.dto.request.RefreshTokenRequest;
import com.glb.campulsebackend.dto.request.RegisterRequest;
import com.glb.campulsebackend.dto.response.AuthResponse;
import com.glb.campulsebackend.entity.User;
import com.glb.campulsebackend.exception.ConflictException;
import com.glb.campulsebackend.exception.UnauthorizedException;
import com.glb.campulsebackend.mapper.UserMapper;
import com.glb.campulsebackend.repository.UserRepository;
import com.glb.campulsebackend.security.JwtService;
import com.glb.campulsebackend.security.LoginRateLimiter;
import com.glb.campulsebackend.service.AuthService;
import com.glb.campulsebackend.service.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

	private static final String BAD_CREDENTIALS = "Invalid email or password";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokens;
	private final LoginRateLimiter rateLimiter;

	/** Compared against when the email is unknown so response time doesn't reveal which emails exist. */
	private final String dummyHash;

	public AuthServiceImpl(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			RefreshTokenService refreshTokens, LoginRateLimiter rateLimiter) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokens = refreshTokens;
		this.rateLimiter = rateLimiter;
		this.dummyHash = passwordEncoder.encode("campulse-timing-equalizer");
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("An account with this email already exists");
		}
		String name = request.fullName() == null || request.fullName().isBlank()
				? nameFromEmail(email)
				: request.fullName().trim();
		User user = users.save(new User(name, email, passwordEncoder.encode(request.password())));
		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		rateLimiter.checkAllowed(email);

		User user = users.findByEmail(email).orElse(null);
		boolean matches = passwordEncoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
		if (user == null || !matches) {
			rateLimiter.recordFailure(email);
			throw new UnauthorizedException(BAD_CREDENTIALS);
		}
		rateLimiter.reset(email);
		return buildAuthResponse(user);
	}

	@Override
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public AuthResponse refresh(RefreshTokenRequest request) {
		User user = refreshTokens.consume(request.refreshToken());
		return buildAuthResponse(user);
	}

	@Override
	public void logout(RefreshTokenRequest request) {
		refreshTokens.revoke(request.refreshToken());
	}

	private AuthResponse buildAuthResponse(User user) {
		return new AuthResponse(
				jwtService.createAccessToken(user),
				refreshTokens.issue(user),
				"Bearer",
				jwtService.accessTtlSeconds(),
				UserMapper.toResponse(user));
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/** "simran.gangwar2@college.edu" -> "Simran Gangwar". */
	static String nameFromEmail(String email) {
		String local = email.substring(0, email.indexOf('@')).replaceAll("[0-9]+", "");
		String name = Arrays.stream(local.split("[._\\-+]+"))
				.filter(part -> !part.isBlank())
				.map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
				.collect(Collectors.joining(" "));
		return name.isBlank() ? "Student" : name;
	}
}
