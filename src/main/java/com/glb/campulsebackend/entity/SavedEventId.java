package com.glb.campulsebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SavedEventId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "event_id")
	private Long eventId;

	protected SavedEventId() {
	}

	public SavedEventId(Long userId, Long eventId) {
		this.userId = userId;
		this.eventId = eventId;
	}

	public Long getUserId() { return userId; }
	public Long getEventId() { return eventId; }

	@Override
	public boolean equals(Object o) {
		return o instanceof SavedEventId other
				&& Objects.equals(userId, other.userId) && Objects.equals(eventId, other.eventId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, eventId);
	}
}
