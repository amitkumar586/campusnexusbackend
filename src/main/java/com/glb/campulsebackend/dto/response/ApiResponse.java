package com.glb.campulsebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Uniform envelope for every API response: {success, message, data | errors}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Map<String, String> errors) {

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static ApiResponse<Void> error(String message) {
		return new ApiResponse<>(false, message, null, null);
	}

	public static ApiResponse<Void> error(String message, Map<String, String> errors) {
		return new ApiResponse<>(false, message, null, errors);
	}
}
