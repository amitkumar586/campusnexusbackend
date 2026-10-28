package com.glb.campulsebackend.security;

import com.glb.campulsebackend.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Issues short-lived signed access tokens. Verification is done by the resource-server JwtDecoder. */
@Service
public class JwtService {

	public static final String ISSUER = "campulse";

	private final JwtEncoder encoder;
	private final Duration accessTtl;

	public JwtService(JwtEncoder encoder, JwtProperties props) {
		this.encoder = encoder;
		this.accessTtl = Duration.ofMinutes(props.accessTtlMinutes());
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plus(accessTtl))
				.claim("role", user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long accessTtlSeconds() {
		return accessTtl.toSeconds();
	}
}
