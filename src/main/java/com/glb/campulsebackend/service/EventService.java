package com.glb.campulsebackend.service;

import com.glb.campulsebackend.dto.request.CreateEventRequest;
import com.glb.campulsebackend.dto.response.EventResponse;
import com.glb.campulsebackend.dto.response.PageResponse;

import java.util.List;

public interface EventService {

	/** Open events (deadline not passed), soonest deadline first. Category "All"/null and blank query mean no filter. */
	PageResponse<EventResponse> list(Long userId, String category, String query, int page, int size);

	/** Open events ranked by interest match, best first. */
	List<EventResponse> recommended(Long userId, int limit);

	EventResponse get(Long userId, Long eventId);

	List<EventResponse> saved(Long userId);

	/** Idempotent. */
	EventResponse save(Long userId, Long eventId);

	/** Idempotent. */
	EventResponse unsave(Long userId, Long eventId);

	EventResponse create(CreateEventRequest request);
}
