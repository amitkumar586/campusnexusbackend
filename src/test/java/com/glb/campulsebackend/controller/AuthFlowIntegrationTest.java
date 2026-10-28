package com.glb.campulsebackend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@college.edu";
	}

	private MvcResult post(String path, String json, int expectedStatus) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().is(expectedStatus)).andReturn();
	}

	private String register(String email) throws Exception {
		return post("/api/v1/auth/register",
				"{\"email\":\"" + email + "\",\"password\":\"password123\"}", 201).getResponse().getContentAsString();
	}

	@Test
	void registerLoginProfileRefreshAndLogoutFlow() throws Exception {
		String email = uniqueEmail();
		String registered = register(email);
		String access = JsonPath.read(registered, "$.data.accessToken");
		String refresh = JsonPath.read(registered, "$.data.refreshToken");
		org.junit.jupiter.api.Assertions.assertEquals(email, JsonPath.read(registered, "$.data.user.email"));
		org.junit.jupiter.api.Assertions.assertFalse(registered.contains("passwordHash"));

		// profile with token
		mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.role").value("STUDENT"));

		// partial profile update; name-only update must keep other fields
		mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"program\":\"B.Tech CSE\",\"studyYear\":\"3rd Year\",\"interests\":[\"AI\",\"Design\"]}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Simran Gangwar\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Simran Gangwar"))
				.andExpect(jsonPath("$.data.program").value("B.Tech CSE"))
				.andExpect(jsonPath("$.data.interests[0]").value("ai"))
				.andExpect(jsonPath("$.data.interests[1]").value("design"));

		// login works, email is case-insensitive
		post("/api/v1/auth/login",
				"{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\"}", 200);

		// refresh rotates the token
		String refreshed = post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 200)
				.getResponse().getContentAsString();
		String newRefresh = JsonPath.read(refreshed, "$.data.refreshToken");
		org.junit.jupiter.api.Assertions.assertNotEquals(refresh, newRefresh);

		// reusing the old token fails AND revokes the whole family (theft detection)
		post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 401);
		post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + newRefresh + "\"}", 401);

		// logout revokes a fresh token
		String login = post("/api/v1/auth/login",
				"{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200).getResponse().getContentAsString();
		String loginRefresh = JsonPath.read(login, "$.data.refreshToken");
		post("/api/v1/auth/logout", "{\"refreshToken\":\"" + loginRefresh + "\"}", 200);
		post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + loginRefresh + "\"}", 401);
	}

	@Test
	void duplicateEmailIsRejectedWithConflict() throws Exception {
		String email = uniqueEmail();
		register(email);
		post("/api/v1/auth/register", "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"password123\"}", 409);
	}

	@Test
	void invalidRegistrationReturnsFieldErrors() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void malformedJsonReturns400WithoutInternals() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{bad"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSameMessage() throws Exception {
		String email = uniqueEmail();
		register(email);
		String wrongPassword = post("/api/v1/auth/login",
				"{\"email\":\"" + email + "\",\"password\":\"nope-nope-nope\"}", 401).getResponse().getContentAsString();
		String unknownEmail = post("/api/v1/auth/login",
				"{\"email\":\"" + uniqueEmail() + "\",\"password\":\"nope-nope-nope\"}", 401).getResponse().getContentAsString();
		org.junit.jupiter.api.Assertions.assertEquals(wrongPassword, unknownEmail);
	}

	@Test
	void repeatedFailedLoginsAreRateLimited() throws Exception {
		String email = uniqueEmail();
		register(email);
		for (int i = 0; i < 5; i++) {
			post("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"bad-password-" + i + "\"}", 401);
		}
		// even the correct password is blocked while the lockout window is active
		post("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 429);
	}

	@Test
	void protectedEndpointsRejectMissingAndTamperedTokens() throws Exception {
		mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));

		String registered = register(uniqueEmail());
		String access = JsonPath.read(registered, "$.data.accessToken");
		String tampered = access.substring(0, access.length() - 4) + "AAAA";
		mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}
}
