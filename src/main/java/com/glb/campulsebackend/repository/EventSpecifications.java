package com.glb.campulsebackend.repository;

import com.glb.campulsebackend.entity.Event;
import com.glb.campulsebackend.entity.EventCategory;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EventSpecifications {

	private static final char ESCAPE = '\\';

	private EventSpecifications() {
	}

	/** Events still open for registration. */
	public static Specification<Event> openAt(Instant now) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deadline"), now);
	}

	public static Specification<Event> hasCategory(EventCategory category) {
		return (root, query, cb) -> cb.equal(root.get("category"), category);
	}

	/** Case-insensitive title search; LIKE wildcards typed by the user are matched literally. */
	public static Specification<Event> titleContains(String text) {
		String escaped = text.toLowerCase()
				.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + escaped + "%", ESCAPE);
	}
}
