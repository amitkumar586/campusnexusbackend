package com.glb.campulsebackend.config;

import com.glb.campulsebackend.entity.Event;
import com.glb.campulsebackend.entity.EventCategory;
import com.glb.campulsebackend.entity.EventMode;
import com.glb.campulsebackend.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Inserts the events the Flutter app showed as hardcoded demo data. Opt-in
 * (app.seed.demo-data=true) and only runs on an empty events table, so it never touches real data.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private final EventRepository events;

	public DemoDataSeeder(EventRepository events) {
		this.events = events;
	}

	@Override
	public void run(String... args) {
		if (events.count() > 0) {
			return;
		}
		Instant now = Instant.now();
		events.save(new Event("Smart India Hackathon", EventCategory.HACKATHON, EventMode.ONLINE,
				"National-level hackathon to build solutions for real problems.",
				now.plus(Duration.ofDays(3)), Set.of("ai", "coding", "entrepreneurship")));
		events.save(new Event("UI/UX Design Workshop", EventCategory.WORKSHOP, EventMode.CAMPUS,
				"Hands-on workshop covering research, wireframing and prototyping.",
				now.plus(Duration.ofDays(6)), Set.of("design", "ux")));
		events.save(new Event("Summer Internship Fair", EventCategory.INTERNSHIP, EventMode.CAMPUS,
				"Meet recruiters and apply for summer internships on campus.",
				now.plus(Duration.ofDays(10)), Set.of("career", "entrepreneurship")));
		log.info("Seeded 3 demo events");
	}
}
