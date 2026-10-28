package com.glb.campulsebackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.jwt")
public record JwtProperties(
		String secret,
		@DefaultValue("15") long accessTtlMinutes,
		@DefaultValue("7") long refreshTtlDays) {
}
