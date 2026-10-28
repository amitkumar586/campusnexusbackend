package com.glb.campulsebackend.service.impl;

import com.glb.campulsebackend.dto.request.LoginRequest;
import com.glb.campulsebackend.dto.request.RegisterRequest;
import com.glb.campulsebackend.dto.response.AuthResponse;
import com.glb.campulsebackend.entity.User;
import com.glb.campulsebackend.exception.ConflictException;
import com.glb.campulsebackend.exception.UnauthorizedException;
import com.glb.campulsebackend.repository.UserRepository;
import com.glb.campulsebackend.security.JwtService;
import com.glb.campulsebackend.security.LoginRateLimiter;
import com.glb.campulsebackend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock UserRepository users;
	@Mock JwtService jwtService;
	@Mock RefreshTokenService refreshTokens;
	@Mock LoginRateLimiter rateLimiter;

	private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
	private AuthServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new AuthServiceImpl(users, encoder, jwtService, refreshTokens, rateLimiter);
	}

	@Test
	void registerHashesPasswordNormalizesEmailAndDerivesName() {
		when(users.existsByEmail("simran.gangwar@college.edu")).thenReturn(false);
		when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(jwtService.createAccessToken(any())).thenReturn("access");
		when(refreshTokens.issue(any())).thenReturn("refresh");

		AuthResponse response = service.register(
				new RegisterRequest(null, "  Simran.Gangwar@College.edu ", "s3cret-pass"));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).save(saved.capture());
		assertEquals("simran.gangwar@college.edu", saved.getValue().getEmail());
		assertEquals("Simran Gangwar", saved.getValue().getFullName());
		assertNotEquals("s3cret-pass", saved.getValue().getPasswordHash());
		assertTrue(encoder.matches("s3cret-pass", saved.getValue().getPasswordHash()));
		assertEquals("access", response.accessToken());
		assertEquals("refresh", response.refreshToken());
	}

	@Test
	void registerRejectsDuplicateEmail() {
		when(users.existsByEmail("a@b.edu")).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.register(new RegisterRequest("A", "a@b.edu", "password123")));
		verify(users, never()).save(any());
	}

	@Test
	void loginWithUnknownEmailFailsAndRecordsFailure() {
		when(users.findByEmail("nobody@b.edu")).thenReturn(Optional.empty());

		assertThrows(UnauthorizedException.class,
				() -> service.login(new LoginRequest("nobody@b.edu", "whatever123")));
		verify(rateLimiter).recordFailure("nobody@b.edu");
		verify(refreshTokens, never()).issue(any());
	}

	@Test
	void loginWithWrongPasswordFailsAndRecordsFailure() {
		User user = new User("A", "a@b.edu", encoder.encode("right-password"));
		when(users.findByEmail("a@b.edu")).thenReturn(Optional.of(user));

		assertThrows(UnauthorizedException.class,
				() -> service.login(new LoginRequest("a@b.edu", "wrong-password")));
		verify(rateLimiter).recordFailure("a@b.edu");
	}

	@Test
	void successfulLoginResetsRateLimiter() {
		User user = new User("A", "a@b.edu", encoder.encode("right-password"));
		when(users.findByEmail("a@b.edu")).thenReturn(Optional.of(user));
		when(jwtService.createAccessToken(any())).thenReturn("access");
		when(refreshTokens.issue(any())).thenReturn("refresh");

		service.login(new LoginRequest("A@B.edu", "right-password"));

		verify(rateLimiter).reset("a@b.edu");
		verify(rateLimiter, never()).recordFailure(anyString());
	}

	@Test
	void nameFromEmailFallsBackToStudent() {
		assertEquals("Student", AuthServiceImpl.nameFromEmail("12345@college.edu"));
		assertEquals("Ravi Kumar", AuthServiceImpl.nameFromEmail("ravi_kumar7@college.edu"));
	}
}
