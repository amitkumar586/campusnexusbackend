package com.glb.campulsebackend.security;

import com.glb.campulsebackend.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory limiter for failed logins, keyed by email. Single-instance only; move to a shared
 * store (e.g. Redis) if the backend is ever scaled horizontally.
 */
@Component
public class LoginRateLimiter {

	static final int MAX_FAILURES = 5;
	private static final Duration WINDOW = Duration.ofMinutes(15);
	private static final int PRUNE_THRESHOLD = 10_000;

	private record Attempts(int failures, Instant windowStart) {
	}

	private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

	public void checkAllowed(String key) {
		Attempts a = attempts.get(key);
		if (a != null && !expired(a) && a.failures() >= MAX_FAILURES) {
			throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
		}
	}

	public void recordFailure(String key) {
		Instant now = Instant.now();
		if (attempts.size() > PRUNE_THRESHOLD) {
			attempts.values().removeIf(this::expired);
		}
		attempts.merge(key, new Attempts(1, now),
				(old, fresh) -> expired(old) ? fresh : new Attempts(old.failures() + 1, old.windowStart()));
	}

	public void reset(String key) {
		attempts.remove(key);
	}

	private boolean expired(Attempts a) {
		return a.windowStart().plus(WINDOW).isBefore(Instant.now());
	}
}
