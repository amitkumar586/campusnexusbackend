package com.glb.campulsebackend.mapper;

import com.glb.campulsebackend.dto.response.EventResponse;
import com.glb.campulsebackend.entity.Event;

public final class EventMapper {

	private EventMapper() {
	}

	/** Must be called inside a transaction: tags are lazily loaded. */
	public static EventResponse toResponse(Event event, Integer matchPercent, boolean saved) {
		return new EventResponse(
				event.getId(),
				event.getTitle(),
				event.getCategory(),
				event.getMode(),
				event.getDescription(),
				event.getDeadline(),
				event.getTags().stream().sorted().toList(),
				matchPercent,
				saved);
	}
}
