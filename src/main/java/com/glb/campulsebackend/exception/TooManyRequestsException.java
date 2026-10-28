package com.glb.campulsebackend.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

	public TooManyRequestsException(String message) {
		super(HttpStatus.TOO_MANY_REQUESTS, message);
	}
}
