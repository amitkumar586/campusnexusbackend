package com.glb.campulsebackend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EventCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EventMode mode;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false)
	private Instant deadline;

	@ElementCollection
	@CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
	@Column(name = "tag", length = 50)
	@BatchSize(size = 50)
	private Set<String> tags = new LinkedHashSet<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Event() {
	}

	public Event(String title, EventCategory category, EventMode mode, String description, Instant deadline,
			Set<String> tags) {
		this.title = title;
		this.category = category;
		this.mode = mode;
		this.description = description;
		this.deadline = deadline;
		this.tags = new LinkedHashSet<>(tags);
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public EventCategory getCategory() { return category; }
	public EventMode getMode() { return mode; }
	public String getDescription() { return description; }
	public Instant getDeadline() { return deadline; }
	public Set<String> getTags() { return tags; }
}
