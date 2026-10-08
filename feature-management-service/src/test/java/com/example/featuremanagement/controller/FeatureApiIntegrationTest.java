package com.example.featuremanagement.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeatureApiIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper objectMapper;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void crudAndOverridesEndToEnd() throws Exception {
		HttpResponse<String> create = send(
				"POST",
				"/api/v1/flags",
				"""
						{
						  "name": "new-checkout",
						  "description": "Checkout flow",
						  "globalEnabled": false,
						  "state": "ACTIVE"
						}
						"""
		);
		assertThat(create.statusCode()).isEqualTo(201);
		assertThat(objectMapper.readTree(create.body()).get("name").asText()).isEqualTo("new-checkout");

		HttpResponse<String> duplicate = send(
				"POST",
				"/api/v1/flags",
				"""
						{"name":"new-checkout","globalEnabled":true}
						"""
		);
		assertThat(duplicate.statusCode()).isEqualTo(409);

		HttpResponse<String> invalidName = send(
				"POST",
				"/api/v1/flags",
				"""
						{"name":"Bad Name","globalEnabled":true}
						"""
		);
		assertThat(invalidName.statusCode()).isEqualTo(400);

		HttpResponse<String> get = send("GET", "/api/v1/flags/new-checkout", null);
		assertThat(get.statusCode()).isEqualTo(200);

		HttpResponse<String> patch = send(
				"PATCH",
				"/api/v1/flags/new-checkout",
				"""
						{"description":"Updated checkout","globalEnabled":true}
						"""
		);
		assertThat(patch.statusCode()).isEqualTo(200);
		JsonNode patched = objectMapper.readTree(patch.body());
		assertThat(patched.get("description").asText()).isEqualTo("Updated checkout");
		assertThat(patched.get("globalEnabled").asBoolean()).isTrue();

		HttpResponse<String> upsertOverride = send(
				"PUT",
				"/api/v1/flags/new-checkout/overrides/user-123",
				"""
						{"enabled":false}
						"""
		);
		assertThat(upsertOverride.statusCode()).isEqualTo(200);
		assertThat(objectMapper.readTree(upsertOverride.body()).get("enabled").asBoolean()).isFalse();

		HttpResponse<String> listOverrides = send("GET", "/api/v1/flags/new-checkout/overrides", null);
		assertThat(listOverrides.statusCode()).isEqualTo(200);
		assertThat(objectMapper.readTree(listOverrides.body()).get("content")).isNotEmpty();

		HttpResponse<String> removeOverride = send(
				"DELETE",
				"/api/v1/flags/new-checkout/overrides/user-123",
				null
		);
		assertThat(removeOverride.statusCode()).isEqualTo(204);

		HttpResponse<String> listAfterRemove = send("GET", "/api/v1/flags/new-checkout/overrides", null);
		assertThat(objectMapper.readTree(listAfterRemove.body()).get("content")).isEmpty();

		HttpResponse<String> missing = send("GET", "/api/v1/flags/does-not-exist", null);
		assertThat(missing.statusCode()).isEqualTo(404);

		HttpResponse<String> delete = send("DELETE", "/api/v1/flags/new-checkout", null);
		assertThat(delete.statusCode()).isEqualTo(204);
	}

	private HttpResponse<String> send(String method, String path, String body) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("X-Actor-Id", "test-admin");

		if (body != null) {
			builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		else {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}

		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}
}
