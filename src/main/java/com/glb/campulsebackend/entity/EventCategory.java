package com.glb.campulsebackend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.glb.campulsebackend.exception.BadRequestException;

import java.util.Arrays;

/** Serialized as the label the Flutter app shows and filters by ("Hackathon", ...). */
public enum EventCategory {
	HACKATHON("Hackathon"),
	WORKSHOP("Workshop"),
	INTERNSHIP("Internship");

	private final String label;

	EventCategory(String label) {
		this.label = label;
	}

	@JsonValue
	public String label() {
		return label;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static EventCategory fromLabel(String value) {
		return Arrays.stream(values())
				.filter(c -> c.label.equalsIgnoreCase(value == null ? "" : value.trim()))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Unknown category: " + value));
	}

	/** Query-parameter form: missing, blank or "All" means no filter. */
	public static EventCategory parseFilter(String value) {
		if (value == null || value.isBlank() || value.trim().equalsIgnoreCase("all")) {
			return null;
		}
		return fromLabel(value);
	}
}
