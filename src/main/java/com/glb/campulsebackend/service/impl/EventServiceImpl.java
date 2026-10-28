package com.glb.campulsebackend.service.impl;

import com.glb.campulsebackend.dto.request.CreateEventRequest;
import com.glb.campulsebackend.dto.response.EventResponse;
import com.glb.campulsebackend.dto.response.PageResponse;
import com.glb.campulsebackend.entity.Event;
import com.glb.campulsebackend.entity.EventCategory;
import com.glb.campulsebackend.entity.SavedEvent;
import com.glb.campulsebackend.entity.SavedEventId;
import com.glb.campulsebackend.exception.ResourceNotFoundException;
import com.glb.campulsebackend.exception.UnauthorizedException;
import com.glb.campulsebackend.mapper.EventMapper;
import com.glb.campulsebackend.repository.EventRepository;
import com.glb.campulsebackend.repository.EventSpecifications;
import com.glb.campulsebackend.repository.SavedEventRepository;
import com.glb.campulsebackend.repository.UserRepository;
import com.glb.campulsebackend.service.EventService;
import com.glb.campulsebackend.service.MatchScoreCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_LIMIT = 20;
	private static final int RECOMMENDATION_POOL = 200;
	private static final int MAX_SAVED = 100;
	private static final Sort BY_DEADLINE = Sort.by("deadline").ascending().and(Sort.by("id").ascending());

	private final EventRepository events;
	private final SavedEventRepository savedEvents;
	private final UserRepository users;

	public EventServiceImpl(EventRepository events, SavedEventRepository savedEvents, UserRepository users) {
		this.events = events;
		this.savedEvents = savedEvents;
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EventResponse> list(Long userId, String category, String query, int page, int size) {
		Set<String> interests = interestsOf(userId);

		Specification<Event> spec = EventSpecifications.openAt(Instant.now());
		EventCategory filter = EventCategory.parseFilter(category);
		if (filter != null) {
			spec = spec.and(EventSpecifications.hasCategory(filter));
		}
		if (query != null && !query.isBlank()) {
			spec = spec.and(EventSpecifications.titleContains(query.trim()));
		}

		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), BY_DEADLINE);
		Page<Event> result = events.findAll(spec, pageable);
		Set<Long> saved = savedIdsAmong(userId, result.getContent());
		return PageResponse.from(result.map(e -> toResponse(e, interests, saved.contains(e.getId()))));
	}

	@Override
	@Transactional(readOnly = true)
	public List<EventResponse> recommended(Long userId, int limit) {
		Set<String> interests = interestsOf(userId);
		Pageable pool = PageRequest.of(0, RECOMMENDATION_POOL, BY_DEADLINE);
		List<Event> open = events.findAll(EventSpecifications.openAt(Instant.now()), pool).getContent();
		Set<Long> saved = savedIdsAmong(userId, open);

		return open.stream()
				.map(e -> toResponse(e, interests, saved.contains(e.getId())))
				.sorted(Comparator.comparing(EventResponse::matchPercent, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(EventResponse::deadline))
				.limit(Math.min(Math.max(limit, 1), MAX_LIMIT))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public EventResponse get(Long userId, Long eventId) {
		Event event = find(eventId);
		boolean saved = savedEvents.existsById(new SavedEventId(userId, eventId));
		return toResponse(event, interestsOf(userId), saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EventResponse> saved(Long userId) {
		Set<String> interests = interestsOf(userId);
		return events.findSavedBy(userId, PageRequest.of(0, MAX_SAVED)).stream()
				.map(e -> toResponse(e, interests, true))
				.toList();
	}

	@Override
	@Transactional
	public EventResponse save(Long userId, Long eventId) {
		Event event = find(eventId);
		SavedEventId id = new SavedEventId(userId, eventId);
		if (!savedEvents.existsById(id)) {
			savedEvents.save(new SavedEvent(userId, eventId));
		}
		return toResponse(event, interestsOf(userId), true);
	}

	@Override
	@Transactional
	public EventResponse unsave(Long userId, Long eventId) {
		Event event = find(eventId);
		SavedEventId id = new SavedEventId(userId, eventId);
		if (savedEvents.existsById(id)) {
			savedEvents.deleteById(id);
		}
		return toResponse(event, interestsOf(userId), false);
	}

	@Override
	@Transactional
	public EventResponse create(CreateEventRequest request) {
		Set<String> tags = request.tags() == null ? Set.of() : request.tags().stream()
				.map(t -> t.trim().toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
		Event event = events.save(new Event(request.title().trim(), request.category(), request.mode(),
				request.description(), request.deadline(), tags));
		return EventMapper.toResponse(event, null, false);
	}

	private Event find(Long eventId) {
		return events.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
	}

	private Set<String> interestsOf(Long userId) {
		return users.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("Account no longer exists"))
				.getInterests();
	}

	private Set<Long> savedIdsAmong(Long userId, Collection<Event> candidates) {
		if (candidates.isEmpty()) {
			return Set.of();
		}
		return savedEvents.findSavedEventIds(userId, candidates.stream().map(Event::getId).toList());
	}

	private static EventResponse toResponse(Event event, Set<String> interests, boolean saved) {
		return EventMapper.toResponse(event, MatchScoreCalculator.score(interests, event.getTags()), saved);
	}
}
