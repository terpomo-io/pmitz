/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.remote.server.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.remote.client.http.PmitzApiKeyAuthenticationProvider;
import io.terpomo.pmitz.remote.client.http.PmitzHttpClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Dockerfile + docker compose wiring for the remote server.
 *
 * This is intentionally an integration test (requires a working Docker daemon).
 *
 * @author Terpomo Software
 */
@Testcontainers
@Tag("docker")
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DockerRemoteServerIntegrationTests {

	private static final Logger LOGGER = LoggerFactory.getLogger(DockerRemoteServerIntegrationTests.class);

	private static final String API_KEY = "test-api-key";
	private static final String DOCKER_IMAGE = "pmitz-local:integration-test";
	private static final String PMITZ_SERVICE = "pmitz";
	private static final int PMITZ_PORT = 8080;

	private static final String PRODUCT_ID = "Library";
	private static final String FEATURE_ID = "Books";
	private static final String LIMIT_ID = "Max books";
	private static final String PLAN_ID = "Basic";
	private static final String USER_ID = "user1";
	private static final String DIRECTORY_GROUP_ID = "group1";
	private static final String SUBSCRIPTION_ID = "sub1";

	private static String baseUrl;
	private static PmitzHttpClient pmitzClient;
	private static HttpClient rawHttpClient;
	private static FeatureRef featureRef;

	static {
		buildDockerImage();
	}

	@Container
	private static final ComposeContainer compose = new ComposeContainer(
			new java.io.File("src/test/resources/docker-compose.integration-test.yml"))
			.withLocalCompose(false)
			.withTailChildContainers(true)
			.withLogConsumer(PMITZ_SERVICE, frame -> LOGGER.info("{}", frame.getUtf8String()))
			.withLogConsumer("postgres", frame -> LOGGER.info("{}", frame.getUtf8String()))
			.withEnv("PMITZ_API_KEY", API_KEY)
			.withExposedService(PMITZ_SERVICE, PMITZ_PORT,
					Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

	@BeforeAll
	static void initClients() {
		String host = compose.getServiceHost(PMITZ_SERVICE, PMITZ_PORT);
		int port = compose.getServicePort(PMITZ_SERVICE, PMITZ_PORT);
		baseUrl = "http://" + host + ":" + port;

		System.setProperty("pmitz.api.key", API_KEY);
		pmitzClient = new PmitzHttpClient(baseUrl, new PmitzApiKeyAuthenticationProvider());
		rawHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		featureRef = new FeatureRef(PRODUCT_ID, FEATURE_ID);
	}

	@Test
	@Order(1)
	void healthEndpointShouldBeReachableWithoutAuth() throws Exception {
		assertNotNull(baseUrl);
		// Health should be reachable without auth.
		HttpRequest healthReq = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/actuator/health"))
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();
		HttpResponse<String> healthResp = rawHttpClient.send(healthReq, HttpResponse.BodyHandlers.ofString());
		assertTrue(healthResp.statusCode() == 200, "Expected 200 from /actuator/health, got " + healthResp.statusCode());
	}

	@Test
	@Order(2)
	void protectedEndpointShouldRejectMissingApiKey() throws Exception {
		// Protected endpoint should reject missing API key.
		HttpRequest unauthReq = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/subscriptions/nonexistent"))
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();
		HttpResponse<Void> unauthResp = rawHttpClient.send(unauthReq, HttpResponse.BodyHandlers.discarding());
		int unauthCode = unauthResp.statusCode();
		assertTrue(unauthCode == 401 || unauthCode == 403, "Expected 401/403 without API key, got " + unauthCode);
	}

	@Test
	@Order(3)
	void shouldUploadProduct() {
		// Upload product (includes plans, needed for subscription-check to return OK)
		String productJson = """
				{
				  "productId": "%s",
				  "features": [{
				    "featureId": "%s",
				    "limits": [{
				      "type": "CountLimit",
				      "id": "%s",
				      "count": 5
				    }]
				  }],
				  "plans": [{
				    "planId": "%s",
				    "includedFeatures": ["%s"]
				  }]
				}
				""".formatted(PRODUCT_ID, FEATURE_ID, LIMIT_ID, PLAN_ID, FEATURE_ID);
		pmitzClient.uploadProduct(new java.io.ByteArrayInputStream(productJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@Test
	@Order(4)
	void shouldRecordAndVerifyUserUsage() {
		var user = new IndividualUser(USER_ID);
		FeatureUsageInfo initialUserUsage = pmitzClient.getLimitsRemainingUnits(featureRef, user);
		assertTrue(initialUserUsage != null, "Expected FeatureUsageInfo for user usage endpoint");
		pmitzClient.recordOrReduce(featureRef, user, Map.of(LIMIT_ID, 2L), false);
		pmitzClient.verifyLimits(featureRef, user, Map.of(LIMIT_ID, 1L));
	}

	@Test
	@Order(5)
	void shouldRecordAndVerifyDirectoryGroupUsage() {
		var group = new DirectoryGroup(DIRECTORY_GROUP_ID);
		FeatureUsageInfo initialGroupUsage = pmitzClient.getLimitsRemainingUnits(featureRef, group);
		assertTrue(initialGroupUsage != null, "Expected FeatureUsageInfo for directory-group usage endpoint");
		pmitzClient.recordOrReduce(featureRef, group, Map.of(LIMIT_ID, 1L), false);
		pmitzClient.verifyLimits(featureRef, group, Map.of(LIMIT_ID, 1L));
	}

	@Test
	@Order(6)
	void shouldCreateAndVerifySubscription() throws Exception {
		// Create subscription via raw HTTP to ensure JSON property is "plans" (server expects setPlans()).
		String subscriptionJson = """
				{
				  "subscriptionId": "%s",
				  "status": "ACTIVE",
				  "plans": {
				    "%s": "%s"
				  }
				}
				""".formatted(SUBSCRIPTION_ID, PRODUCT_ID, PLAN_ID);
		HttpRequest createSubReq = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/subscriptions"))
				.timeout(Duration.ofSeconds(20))
				.header("Content-Type", "application/json")
				.header("X-Api-Key", API_KEY)
				.POST(HttpRequest.BodyPublishers.ofString(subscriptionJson))
				.build();
		HttpResponse<Void> createSubResp = rawHttpClient.send(createSubReq, HttpResponse.BodyHandlers.discarding());
		assertTrue(createSubResp.statusCode() == 200, "Expected 200 when creating subscription, got " + createSubResp.statusCode());

		assertTrue(pmitzClient.findSubscription(SUBSCRIPTION_ID).isPresent(), "Expected created subscription to be persisted and retrievable");
		pmitzClient.updateSubscriptionStatus(SUBSCRIPTION_ID, SubscriptionStatus.ACTIVE);

		FeatureUsageInfo initialSubscriptionUsage = pmitzClient.getLimitsRemainingUnits(featureRef, new Subscription(SUBSCRIPTION_ID));
		assertTrue(initialSubscriptionUsage != null, "Expected FeatureUsageInfo for subscription usage endpoint");

		SubscriptionVerifDetail verif = pmitzClient.verifySubscription(featureRef, new Subscription(SUBSCRIPTION_ID));
		assertTrue(verif != null, "Expected subscription-check response");
		assertTrue(verif.isFeatureAllowed(), "Expected featureAllowed=true for valid subscription + plan + included feature");
	}

	@Test
	@Order(7)
	void shouldReturnLimitExceededWhenOverLimit() {
		// Verify limit exceeded path returns 422 (remote client throws LimitExceededException).
		var user = new IndividualUser(USER_ID);
		assertThrows(io.terpomo.pmitz.core.exception.LimitExceededException.class,
				() -> pmitzClient.recordOrReduce(featureRef, user, Map.of(LIMIT_ID, 10L), false),
				"Expected LimitExceededException when exceeding CountLimit");
	}

	@Test
	@Order(8)
	void shouldRemoveProduct() {
		pmitzClient.removeProduct(PRODUCT_ID);
	}

	private static void buildDockerImage() {
		ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", DOCKER_IMAGE, "-f", "../Dockerfile", "..");
		pb.directory(new java.io.File("."));
		pb.redirectErrorStream(true);

		try {
			Process p = pb.start();
			String output = new String(p.getInputStream().readAllBytes());
			int code = p.waitFor();
			if (code != 0) {
				throw new IllegalStateException("docker build failed (" + code + "):\n" + output);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while building Docker image for integration tests", ex);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to build Docker image for integration tests", ex);
		}
	}
}

