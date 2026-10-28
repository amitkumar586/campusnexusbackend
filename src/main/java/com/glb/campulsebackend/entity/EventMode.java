package com.glb.campulsebackend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.glb.campulsebackend.exception.BadRequestException;

import java.util.Arrays;

public enum EventMode {
	ONLINE("Online"),
	CAMPUS("Campus"),
	HYBRID("Hybrid");

	private final String label;

	EventMode(String label) {
		this.label = label;
	}

	@JsonValue
	public String label() {
		return label;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static EventMode fromLabel(String value) {
		return Arrays.stream(values())
				.filter(m -> m.label.equalsIgnoreCase(value == null ? "" : value.trim()))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Unknown mode: " + value));
	}
}
