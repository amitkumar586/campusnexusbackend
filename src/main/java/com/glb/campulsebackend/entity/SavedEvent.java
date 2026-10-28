package com.glb.campulsebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "saved_events")
public class SavedEvent {

	@EmbeddedId
	private SavedEventId id;

	@CreationTimestamp
	@Column(name = "saved_at", nullable = false, updatable = false)
	private Instant savedAt;

	protected SavedEvent() {
	}

	public SavedEvent(Long userId, Long eventId) {
		this.id = new SavedEventId(userId, eventId);
	}

	public SavedEventId getId() { return id; }
}
