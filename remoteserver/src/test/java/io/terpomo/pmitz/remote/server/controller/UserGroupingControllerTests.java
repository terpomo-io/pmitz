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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.remote.server.security.ApiKeyAuthentication;
import io.terpomo.pmitz.remote.server.security.AuthenticationService;
import io.terpomo.pmitz.remote.server.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserGroupingController.class)
@Import({SecurityConfig.class})
class UserGroupingControllerTests {

	private final String productId = "product1";
	private final String featureId = "feature1";
	private final Product product = new Product(productId);
	private final Feature feature = new Feature(product, featureId);

	@MockitoBean
	AuthenticationService authenticationService;
	@MockitoBean
	FeatureUsageTracker featureUsageTracker;
	@MockitoBean
	ProductRepository productRepository;
	@MockitoBean
	SubscriptionVerifier subscriptionVerifier;
	@Autowired
	MockMvc mockMvc;

	private final ApiKeyAuthentication apiKeyAuthentication = new ApiKeyAuthentication("test-api-key", AuthorityUtils.NO_AUTHORITIES);

	private static Stream<Arguments> usageUrlsAndUserGroupingsProvider() {
		return Stream.of(
				Arguments.of("/users/theId/usage/product1/feature1", new IndividualUser("theId")),
				Arguments.of("/directory-groups/theId/usage/product1/feature1", new DirectoryGroup("theId")),
				Arguments.of("/subscriptions/theId/usage/product1/feature1", new Subscription("theId")));
	}

	private static Stream<Arguments> verifyLimitsUrlsAndUserGroupingsProvider() {
		return Stream.of(
				Arguments.of("/users/theId/limits-check/product1/feature1", new IndividualUser("theId")),
				Arguments.of("/directory-groups/theId/limits-check/product1/feature1", new DirectoryGroup("theId")),
				Arguments.of("/subscriptions/theId/limits-check/product1/feature1", new Subscription("theId")));
	}

	private static Stream<Arguments> subscriptionCheckUrlsAndUserGroupingsProvider() {
		return Stream.of(
				Arguments.of("/users/theId/subscription-check/product1/feature1", new IndividualUser("theId")),
				Arguments.of("/directory-groups/theId/subscription-check/product1/feature1", new DirectoryGroup("theId")),
				Arguments.of("/subscriptions/theId/subscription-check/product1/feature1", new Subscription("theId")));
	}

	@BeforeEach
	void setup() {
		doReturn(Optional.of(product)).when(productRepository).getProductById(productId);
		doReturn(Optional.of(feature)).when(productRepository).getFeature(product, featureId);
	}

	@ParameterizedTest
	@MethodSource("verifyLimitsUrlsAndUserGroupingsProvider")
	void verifyLimitsShouldReturnUsageInfoIncludingAdditionalUnits(String url, UserGrouping userGrouping) throws Exception {
		var limits = Map.of("limit1", 5L, "limit2", 3L);
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, limits);

		ArgumentMatcher<Map<String, Long>> unitsArgMatcher = arg -> arg != null && arg.getOrDefault("limit1", Long.MAX_VALUE).equals(1L)
				&& arg.getOrDefault("limit2", Long.MAX_VALUE).equals(2L);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));

		doReturn(featureUsageInfo).when(featureUsageTracker).verifyLimits(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));

		String jsonContent = """
				{
					"limit1" : 1,
					"limit2" : 2
				}
				""";

		String expectedJson = """
				{
					"featureStatus" : "AVAILABLE",
					"remainingUsageUnits" : {
						"limit1" : 5,
						"limit2" : 3
					}
				}
				""";
		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(featureUsageTracker, times(1)).verifyLimits(eq(feature), eq(userGrouping), argThat(unitsArgMatcher));

	}

	@ParameterizedTest
	@MethodSource("verifyLimitsUrlsAndUserGroupingsProvider")
	void verifyLimitsShouldReturnStatus401WhenAuthenticationFails(String url, UserGrouping userGrouping) throws Exception {
		when(authenticationService.getAuthentication(any())).thenReturn(null);
		String jsonContent = """
				{
					"limit1" : 1,
					"limit2" : 2
				}
				""";

		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().is(401));

		verify(featureUsageTracker, never()).verifyLimits(any(), any(), any());

	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void recordOrReduceFeatureUsageShouldReturnStatus200WhenRecordAndLimitNotExceeded(String url, UserGrouping userGrouping) throws Exception {

		ArgumentMatcher<Map<String, Long>> unitsArgMatcher = arg -> arg != null && arg.getOrDefault("limit1", Long.MAX_VALUE).equals(1L)
				&& arg.getOrDefault("limit2", Long.MAX_VALUE).equals(2L);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));

		doNothing().when(featureUsageTracker).recordFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));

		String jsonContent = """
				{
					"reduceUnits" : false,
					"units" : {
						"limit1" : 1,
						"limit2" : 2
					}
				}
				""";

		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		verify(featureUsageTracker, times(1)).recordFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));
	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void recordOrReduceFeatureUsageShouldReturnStatus422WhenRecordAndLimitExceeded(String url, UserGrouping userGrouping) throws Exception {
		ArgumentMatcher<Map<String, Long>> unitsArgMatcher = arg -> arg != null && arg.getOrDefault("limit1", Long.MAX_VALUE).equals(1L)
				&& arg.getOrDefault("limit2", Long.MAX_VALUE).equals(2L);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));

		var exception = new LimitExceededException("Limit exceeded", feature, userGrouping);
		doThrow(exception).when(featureUsageTracker).recordFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));

		String jsonContent = """
				{
					"reduceUnits" : false,
					"units" : {
						"limit1" : 1,
						"limit2" : 2
					}
				}
				""";

		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().is(422));

		verify(featureUsageTracker, times(1)).recordFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));
	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void recordOrReduceFeatureUsageShouldReturnStatus200WhenReduce(String url, UserGrouping userGrouping) throws Exception {
		ArgumentMatcher<Map<String, Long>> unitsArgMatcher = arg -> arg != null && arg.getOrDefault("limit1", Long.MAX_VALUE).equals(1L)
				&& arg.getOrDefault("limit2", Long.MAX_VALUE).equals(2L);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));

		doNothing().when(featureUsageTracker).reduceFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));

		String jsonContent = """
				{
					"reduceUnits" : true,
					"units" : {
						"limit1" : 1,
						"limit2" : 2
					}
				}
				""";

		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		verify(featureUsageTracker, times(1)).reduceFeatureUsage(eq(feature), eq(userGrouping),
				argThat(unitsArgMatcher));
	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void recordOrReduceFeatureUsageShouldReturnStatus401WhenAuthenticationFails(String url, UserGrouping userGrouping) throws Exception {
		when(authenticationService.getAuthentication(any())).thenReturn(null);
		String jsonContent = """
				{
					"reduceUnits" : true,
					"units" : {
						"limit1" : 1,
						"limit2" : 2
					}
				}
				""";

		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().is(401));

		verify(featureUsageTracker, never()).reduceFeatureUsage(any(), any(), any());
	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void verifyFeatureUsageShouldReturnFeatureUsageInfo(String url, UserGrouping userGrouping) throws Exception {
		var limits = Map.of("limit1", 15L, "limit2", 10L);
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, limits);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));

		doReturn(featureUsageInfo).when(featureUsageTracker).getUsageInfo(feature, userGrouping);

		String expectedJson = """
				{
					"featureStatus" : "AVAILABLE",
					"remainingUsageUnits" : {
						"limit1" : 15,
						"limit2" : 10
					}
				}
				""";

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(featureUsageTracker, times(1)).getUsageInfo(feature, userGrouping);

	}

	@ParameterizedTest
	@MethodSource("usageUrlsAndUserGroupingsProvider")
	void verifyFeatureUsageShouldReturnStatus401WhenAuthenticationFails(String url, UserGrouping userGrouping) throws Exception {
		when(authenticationService.getAuthentication(any())).thenReturn(null);
		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().is(401));

		verify(featureUsageTracker, never()).getUsageInfo(any(), any());
	}

	@ParameterizedTest
	@MethodSource("subscriptionCheckUrlsAndUserGroupingsProvider")
	void verifySubscriptionShouldReturnVerifDetailWhenFeatureAllowed(String url, UserGrouping userGrouping) throws Exception {
		var verifDetail = SubscriptionVerifDetail.verificationOk();

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));
		doReturn(verifDetail).when(subscriptionVerifier).verifyEntitlement(feature, userGrouping);

		String expectedJson = """
				{
					"featureAllowed" : true,
					"errorCause" : null
				}
				""";

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(subscriptionVerifier, times(1)).verifyEntitlement(feature, userGrouping);
	}

	@ParameterizedTest
	@MethodSource("subscriptionCheckUrlsAndUserGroupingsProvider")
	void verifySubscriptionShouldReturnVerifDetailWhenInvalidSubscription(String url, UserGrouping userGrouping) throws Exception {
		var verifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));
		doReturn(verifDetail).when(subscriptionVerifier).verifyEntitlement(feature, userGrouping);

		String expectedJson = """
				{
					"featureAllowed" : false,
					"errorCause" : "INVALID_SUBSCRIPTION"
				}
				""";

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(subscriptionVerifier, times(1)).verifyEntitlement(feature, userGrouping);
	}

	@ParameterizedTest
	@MethodSource("subscriptionCheckUrlsAndUserGroupingsProvider")
	void verifySubscriptionShouldReturnVerifDetailWhenProductNotAllowed(String url, UserGrouping userGrouping) throws Exception {
		var verifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.PRODUCT_NOT_ALLOWED);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));
		doReturn(verifDetail).when(subscriptionVerifier).verifyEntitlement(feature, userGrouping);

		String expectedJson = """
				{
					"featureAllowed" : false,
					"errorCause" : "PRODUCT_NOT_ALLOWED"
				}
				""";

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(subscriptionVerifier, times(1)).verifyEntitlement(feature, userGrouping);
	}

	@ParameterizedTest
	@MethodSource("subscriptionCheckUrlsAndUserGroupingsProvider")
	void verifySubscriptionShouldReturnVerifDetailWhenFeatureNotAllowed(String url, UserGrouping userGrouping) throws Exception {
		var verifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED);

		doReturn(apiKeyAuthentication).when(authenticationService).getAuthentication(any(HttpServletRequest.class));
		doReturn(verifDetail).when(subscriptionVerifier).verifyEntitlement(feature, userGrouping);

		String expectedJson = """
				{
					"featureAllowed" : false,
					"errorCause" : "FEATURE_NOT_ALLOWED"
				}
				""";

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().isOk())
				.andExpect(content().json(expectedJson));

		verify(subscriptionVerifier, times(1)).verifyEntitlement(feature, userGrouping);
	}

	@ParameterizedTest
	@MethodSource("subscriptionCheckUrlsAndUserGroupingsProvider")
	void verifySubscriptionShouldReturnStatus401WhenAuthenticationFails(String url, UserGrouping userGrouping) throws Exception {
		when(authenticationService.getAuthentication(any())).thenReturn(null);

		mockMvc.perform(get(url)
						.contentType("application/json"))
				.andExpect(status().is(401));

		verify(subscriptionVerifier, never()).verifyEntitlement(any(), any());
	}
}
