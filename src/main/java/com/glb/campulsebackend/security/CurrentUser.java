package com.glb.campulsebackend.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extracts the authenticated user's id from a verified JWT; never trust ids from request bodies. */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long id(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
