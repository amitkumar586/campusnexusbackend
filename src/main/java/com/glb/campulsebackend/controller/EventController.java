package com.glb.campulsebackend.controller;

import com.glb.campulsebackend.dto.request.CreateEventRequest;
import com.glb.campulsebackend.dto.response.ApiResponse;
import com.glb.campulsebackend.dto.response.EventResponse;
import com.glb.campulsebackend.dto.response.PageResponse;
import com.glb.campulsebackend.security.CurrentUser;
import com.glb.campulsebackend.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public ApiResponse<PageResponse<EventResponse>> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String category,
			@RequestParam(name = "q", required = false) String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok("Events retrieved",
				eventService.list(CurrentUser.id(jwt), category, query, page, size));
	}

	@GetMapping("/recommended")
	public ApiResponse<List<EventResponse>> recommended(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "5") int limit) {
		return ApiResponse.ok("Recommended events retrieved", eventService.recommended(CurrentUser.id(jwt), limit));
	}

	@GetMapping("/saved")
	public ApiResponse<List<EventResponse>> saved(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok("Saved events retrieved", eventService.saved(CurrentUser.id(jwt)));
	}

	@GetMapping("/{id}")
	public ApiResponse<EventResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ApiResponse.ok("Event retrieved", eventService.get(CurrentUser.id(jwt), id));
	}

	@PostMapping("/{id}/save")
	public ApiResponse<EventResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ApiResponse.ok("Opportunity saved", eventService.save(CurrentUser.id(jwt), id));
	}

	@DeleteMapping("/{id}/save")
	public ApiResponse<EventResponse> unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ApiResponse.ok("Opportunity removed", eventService.unsave(CurrentUser.id(jwt), id));
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Event created", eventService.create(request)));
	}
}
