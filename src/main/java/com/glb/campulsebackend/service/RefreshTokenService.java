package com.glb.campulsebackend.service;

import com.glb.campulsebackend.entity.RefreshToken;
import com.glb.campulsebackend.entity.User;
import com.glb.campulsebackend.exception.UnauthorizedException;
import com.glb.campulsebackend.repository.RefreshTokenRepository;
import com.glb.campulsebackend.security.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Opaque, single-use, rotating refresh tokens. Only SHA-256 hashes are persisted. */
@Service
public class RefreshTokenService {

	private static final String INVALID = "Invalid or expired refresh token";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository repository;
	private final Duration ttl;

	public RefreshTokenService(RefreshTokenRepository repository, JwtProperties props) {
		this.repository = repository;
		this.ttl = Duration.ofDays(props.refreshTtlDays());
	}

	@Transactional
	public String issue(User user) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		repository.save(new RefreshToken(user, hash(raw), Instant.now().plus(ttl)));
		return raw;
	}

	/**
	 * Consumes a token (it can never be used again) and returns its owner. Presenting an already
	 * used token is treated as theft: every refresh token of that user is revoked.
	 */
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public User consume(String raw) {
		RefreshToken token = repository.findByTokenHash(hash(raw))
				.orElseThrow(() -> new UnauthorizedException(INVALID));
		if (repository.revokeIfActive(token.getId()) == 0) {
			repository.revokeAllForUser(token.getUser().getId());
			throw new UnauthorizedException(INVALID);
		}
		if (token.getExpiresAt().isBefore(Instant.now())) {
			throw new UnauthorizedException(INVALID);
		}
		return token.getUser();
	}

	@Transactional
	public void revoke(String raw) {
		repository.findByTokenHash(hash(raw)).ifPresent(t -> repository.revokeIfActive(t.getId()));
	}

	static String hash(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
