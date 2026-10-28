package com.glb.campulsebackend.dto.response;

import com.glb.campulsebackend.entity.EventCategory;
import com.glb.campulsebackend.entity.EventMode;

import java.time.Instant;
import java.util.List;

/**
 * {@code matchPercent} is null when it can't be computed (user has no interests or the event has no tags);
 * clients should hide the badge then. {@code deadline} is UTC; clients format "in 3 days" themselves.
 */
public record EventResponse(
		Long id,
		String title,
		EventCategory category,
		EventMode mode,
		String description,
		Instant deadline,
		List<String> tags,
		Integer matchPercent,
		boolean saved) {
}
