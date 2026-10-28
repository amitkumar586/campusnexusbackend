package com.glb.campulsebackend.controller;

import com.glb.campulsebackend.entity.Event;
import com.glb.campulsebackend.entity.EventCategory;
import com.glb.campulsebackend.entity.EventMode;
import com.glb.campulsebackend.repository.EventRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class EventFlowIntegrationTest {

	@Autowired WebApplicationContext context;
	@Autowired EventRepository events;
	@Autowired JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	/** Registers a fresh user and returns its access token. */
	private String newUserToken() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@college.edu";
		String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.accessToken");
	}

	private String adminToken() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@college.edu";
		mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"password123\"}")).andExpect(status().isCreated());
		jdbc.update("update users set role = 'ADMIN' where email = ?", email);
		String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.accessToken");
	}

	private Event event(String title, EventCategory cat, long daysFromNow, String... tags) {
		return events.save(new Event(title, cat, EventMode.CAMPUS, "desc",
				Instant.now().plus(Duration.ofDays(daysFromNow)), Set.of(tags)));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	@Test
	void listFiltersByCategoryAndSearchAndHidesPastEvents() throws Exception {
		String key = "k" + UUID.randomUUID().toString().substring(0, 8);
		event(key + " Hack", EventCategory.HACKATHON, 3, "ai");
		event(key + " Workshop", EventCategory.WORKSHOP, 6, "design");
		event(key + " Old Fair", EventCategory.INTERNSHIP, -2, "career");
		String token = newUserToken();

		// past event hidden; soonest deadline first
		mvc.perform(get("/api/v1/events").param("q", key).header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalItems").value(2))
				.andExpect(jsonPath("$.data.items[0].title").value(key + " Hack"))
				.andExpect(jsonPath("$.data.items[0].category").value("Hackathon"))
				.andExpect(jsonPath("$.data.items[0].mode").value("Campus"))
				.andExpect(jsonPath("$.data.items[1].title").value(key + " Workshop"));

		// category filter is case-insensitive, "All" means everything
		mvc.perform(get("/api/v1/events").param("q", key).param("category", "workshop")
						.header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.totalItems").value(1));
		mvc.perform(get("/api/v1/events").param("q", key).param("category", "All")
						.header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.totalItems").value(2));

		// search is case-insensitive and LIKE wildcards are literal
		mvc.perform(get("/api/v1/events").param("q", key.toUpperCase()).header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.totalItems").value(2));
		mvc.perform(get("/api/v1/events").param("q", "%").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.totalItems").value(0));

		// unknown category -> 400
		mvc.perform(get("/api/v1/events").param("category", "Party").header("Authorization", bearer(token)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void matchPercentIsNullUntilUserHasInterestsThenReflectsOverlap() throws Exception {
		String key = "m" + UUID.randomUUID().toString().substring(0, 8);
		event(key + " Hack", EventCategory.HACKATHON, 3, "ai", "coding", "entrepreneurship");
		String token = newUserToken();

		mvc.perform(get("/api/v1/events").param("q", key).header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.items[0].matchPercent").doesNotExist());

		mvc.perform(patch("/api/v1/me").header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"interests\":[\"AI\",\"Design\",\"Entrepreneurship\"]}"))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/events").param("q", key).header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.data.items[0].matchPercent").value(80));
	}

	@Test
	void recommendedRanksByMatchWithUnknownLast() throws Exception {
		String key = "r" + UUID.randomUUID().toString().substring(0, 8);
		event(key + " low", EventCategory.WORKSHOP, 1, "music");
		event(key + " high", EventCategory.HACKATHON, 9, "ai");
		event(key + " untagged", EventCategory.INTERNSHIP, 2);
		String token = newUserToken();
		mvc.perform(patch("/api/v1/me").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"interests\":[\"ai\"]}"));

		String body = mvc.perform(get("/api/v1/events/recommended").param("limit", "20")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		java.util.List<String> titles = JsonPath.read(body, "$.data[?(@.title =~ /" + key + ".*/)].title");
		assertEquals(java.util.List.of(key + " high", key + " low", key + " untagged"), titles);
	}

	@Test
	void saveAndUnsaveAreIdempotentAndPerUser() throws Exception {
		Event e = event("save-" + UUID.randomUUID(), EventCategory.HACKATHON, 5, "ai");
		String alice = newUserToken();
		String bob = newUserToken();
		String path = "/api/v1/events/" + e.getId();

		mvc.perform(get(path).header("Authorization", bearer(alice)))
				.andExpect(jsonPath("$.data.saved").value(false));

		for (int i = 0; i < 2; i++) { // saving twice must not fail
			mvc.perform(MockMvcRequestBuilders.post(path + "/save").header("Authorization", bearer(alice)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.saved").value(true));
		}
		mvc.perform(get("/api/v1/events/saved").header("Authorization", bearer(alice)))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].id").value(e.getId()));

		// bob's view is unaffected
		mvc.perform(get(path).header("Authorization", bearer(bob)))
				.andExpect(jsonPath("$.data.saved").value(false));
		mvc.perform(get("/api/v1/events/saved").header("Authorization", bearer(bob)))
				.andExpect(jsonPath("$.data.length()").value(0));

		for (int i = 0; i < 2; i++) { // unsaving twice must not fail
			mvc.perform(delete(path + "/save").header("Authorization", bearer(alice)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.data.saved").value(false));
		}
		mvc.perform(get("/api/v1/events/saved").header("Authorization", bearer(alice)))
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void missingAndMalformedIdsReturn404And400() throws Exception {
		String token = newUserToken();
		mvc.perform(get("/api/v1/events/999999").header("Authorization", bearer(token)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Event not found"));
		mvc.perform(MockMvcRequestBuilders.post("/api/v1/events/999999/save").header("Authorization", bearer(token)))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/events/abc").header("Authorization", bearer(token)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
		mvc.perform(get("/api/v1/does-not-exist").header("Authorization", bearer(token)))
				.andExpect(status().isNotFound());
	}

	@Test
	void eventsRequireAuthentication() throws Exception {
		mvc.perform(get("/api/v1/events")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/events/saved")).andExpect(status().isUnauthorized());
	}

	@Test
	void onlyAdminsCanCreateEventsAndInputIsValidated() throws Exception {
		String json = "{\"title\":\"Robotics Meetup\",\"category\":\"workshop\",\"mode\":\"Hybrid\","
				+ "\"deadline\":\"" + Instant.now().plus(Duration.ofDays(4)) + "\",\"tags\":[\"Robotics\",\" AI \"]}";

		mvc.perform(MockMvcRequestBuilders.post("/api/v1/events").header("Authorization", bearer(newUserToken()))
						.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isForbidden());

		String admin = adminToken();
		mvc.perform(MockMvcRequestBuilders.post("/api/v1/events").header("Authorization", bearer(admin))
						.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.category").value("Workshop"))
				.andExpect(jsonPath("$.data.mode").value("Hybrid"))
				.andExpect(jsonPath("$.data.tags[0]").value("ai"))
				.andExpect(jsonPath("$.data.tags[1]").value("robotics"));

		mvc.perform(MockMvcRequestBuilders.post("/api/v1/events").header("Authorization", bearer(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"\",\"category\":\"Hackathon\",\"mode\":\"Online\",\"deadline\":\"2020-01-01T00:00:00Z\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.title").exists())
				.andExpect(jsonPath("$.errors.deadline").exists());
	}
}
