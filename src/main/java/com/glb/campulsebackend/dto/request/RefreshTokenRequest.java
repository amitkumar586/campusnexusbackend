package com.glb.campulsebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
		@NotBlank(message = "Refresh token is required") @Size(max = 200) String refreshToken) {
}
