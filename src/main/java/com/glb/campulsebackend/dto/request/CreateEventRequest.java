package com.glb.campulsebackend.dto.request;

import com.glb.campulsebackend.entity.EventCategory;
import com.glb.campulsebackend.entity.EventMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
		@NotBlank(message = "Title is required") @Size(max = 150, message = "Title must be at most 150 characters") String title,
		@NotNull(message = "Category is required") EventCategory category,
		@NotNull(message = "Mode is required") EventMode mode,
		@Size(max = 2000, message = "Description must be at most 2000 characters") String description,
		@NotNull(message = "Deadline is required") @Future(message = "Deadline must be in the future") Instant deadline,
		@Size(max = 10, message = "At most 10 tags") List<@NotBlank @Size(max = 50) String> tags) {
}
