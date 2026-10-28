package com.glb.campulsebackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The app's sign-up form only collects email and password, so fullName is optional. */
public record RegisterRequest(
		@Size(max = 100, message = "Name must be at most 100 characters") String fullName,
		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email address")
		@Size(max = 255, message = "Email is too long") String email,
		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password) {
}
