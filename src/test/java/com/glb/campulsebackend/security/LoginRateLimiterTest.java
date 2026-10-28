package com.glb.campulsebackend.security;

import com.glb.campulsebackend.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

	private final LoginRateLimiter limiter = new LoginRateLimiter();

	@Test
	void blocksAfterMaxFailuresAndAllowsAgainAfterReset() {
		for (int i = 0; i < LoginRateLimiter.MAX_FAILURES; i++) {
			assertDoesNotThrow(() -> limiter.checkAllowed("a@b.edu"));
			limiter.recordFailure("a@b.edu");
		}
		assertThrows(TooManyRequestsException.class, () -> limiter.checkAllowed("a@b.edu"));
		assertDoesNotThrow(() -> limiter.checkAllowed("other@b.edu"));

		limiter.reset("a@b.edu");
		assertDoesNotThrow(() -> limiter.checkAllowed("a@b.edu"));
	}
}
