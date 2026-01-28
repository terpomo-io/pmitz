/*
 * Copyright 2023-2025 the original author or authors.
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

package io.terpomo.pmitz.remote.server.controller;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.remote.server.security.ApiKeyAuthentication;
import io.terpomo.pmitz.remote.server.security.AuthenticationService;
import io.terpomo.pmitz.remote.server.security.SecurityConfig;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriptionController.class)
@ContextConfiguration
@Import({SecurityConfig.class})
class SubscriptionControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	AuthenticationService authenticationService;

	@MockitoBean
	SubscriptionRepository subscriptionRepository;

	private final ApiKeyAuthentication apiKeyAuthentication = new ApiKeyAuthentication("test-api-key", AuthorityUtils.NO_AUTHORITIES);

	@Test
	void createSubscriptionShouldAddSubscriptionToRepository() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.empty());

		String jsonContent = """
				{
					"subscriptionId": "sub001",
					"status": "ACTIVE"
				}
				""";

		mockMvc.perform(post("/subscriptions")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository).create(subscriptionCaptor.capture());

		var subscription = subscriptionCaptor.getValue();
		assertThat(subscription).isNotNull();
		assertThat(subscription.getSubscriptionId()).isEqualTo("sub001");
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
	}

	@Test
	void createSubscriptionWhenAlreadyExistsShouldReturnStatus409() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		var existingSubscription = new Subscription("sub001");
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.of(existingSubscription));

		String jsonContent = """
				{
					"subscriptionId": "sub001",
					"status": "ACTIVE"
				}
				""";

		mockMvc.perform(post("/subscriptions")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isConflict());

		verify(subscriptionRepository, never()).create(any());
	}

	@Test
	void createSubscriptionWhenAuthenticationFailsShouldReturnStatus401() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(null);

		String jsonContent = """
				{
					"subscriptionId": "sub001",
					"status": "ACTIVE"
				}
				""";

		mockMvc.perform(post("/subscriptions")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isUnauthorized());

		verify(subscriptionRepository, never()).create(any());
	}

	@Test
	void findSubscriptionShouldReturnSubscriptionWhenFound() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		var subscription = new Subscription("sub001");
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.of(subscription));

		mockMvc.perform(get("/subscriptions/sub001")
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value("sub001"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		verify(subscriptionRepository).find("sub001");
	}

	@Test
	void findSubscriptionShouldReturnStatus404WhenNotFound() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.empty());

		mockMvc.perform(get("/subscriptions/sub001")
						.contentType("application/json"))
				.andExpect(status().isNotFound());

		verify(subscriptionRepository).find("sub001");
	}

	@Test
	void findSubscriptionWhenAuthenticationFailsShouldReturnStatus401() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(null);

		mockMvc.perform(get("/subscriptions/sub001")
						.contentType("application/json"))
				.andExpect(status().isUnauthorized());

		verify(subscriptionRepository, never()).find(any());
	}

	@Test
	void updateSubscriptionStatusShouldUpdateStatusInRepository() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		var subscription = new Subscription("sub001");
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.of(subscription));

		String jsonContent = """
				{
					"status": "SUSPENDED"
				}
				""";

		mockMvc.perform(patch("/subscriptions/sub001/status")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		verify(subscriptionRepository).updateStatus(subscription, SubscriptionStatus.SUSPENDED);
	}

	@Test
	void updateSubscriptionStatusShouldReturnStatus404WhenNotFound() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.empty());

		String jsonContent = """
				{
					"status": "SUSPENDED"
				}
				""";

		mockMvc.perform(patch("/subscriptions/sub001/status")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isNotFound());

		verify(subscriptionRepository, never()).updateStatus(any(), any());
	}

	@Test
	void updateSubscriptionStatusWhenAuthenticationFailsShouldReturnStatus401() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(null);

		String jsonContent = """
				{
					"status": "SUSPENDED"
				}
				""";

		mockMvc.perform(patch("/subscriptions/sub001/status")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isUnauthorized());

		verify(subscriptionRepository, never()).updateStatus(any(), any());
	}

	@Test
	void updateSubscriptionStatusToCancelledShouldWork() throws Exception {
		when(authenticationService.getAuthentication(any(HttpServletRequest.class))).thenReturn(apiKeyAuthentication);
		var subscription = new Subscription("sub001");
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.find("sub001")).thenReturn(Optional.of(subscription));

		String jsonContent = """
				{
					"status": "CANCELLED"
				}
				""";

		mockMvc.perform(patch("/subscriptions/sub001/status")
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		verify(subscriptionRepository).updateStatus(subscription, SubscriptionStatus.CANCELLED);
	}
}
