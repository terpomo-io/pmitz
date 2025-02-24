/*
 * Copyright 2023-2024 the original author or authors.
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

package io.terpomo.pmitz.remote.client.http;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.FeatureNotFoundException;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.remote.client.RemoteCallException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class PmitzHttpClientTests {

	private String jsonRequestBody = """
			{
				"reduceUnits" : false,
				"units" : {
					"limit1" : 1,
					"limit2" : 2
				}
			}
			""";

	@ParameterizedTest
	@MethodSource({"userGroupingsProvider"})
	void getUsageInfoAndRemoteResponse200ShouldParseHttpResponse(UserGrouping userGrouping, String endpoint, WireMockRuntimeInfo wmRuntimeInfo) {
		String featureId = "newPicUpload";
		String productId = "picUpload";

		String jsonResponse = """
				{
					"featureStatus" : "AVAILABLE",
					"remainingUsageUnits" : {
						"limit1" : 10,
						"limit2" : 6
					}
				}
				""";

		stubFor(get(endpoint + "/limits/picUpload/newPicUpload")
				.willReturn(aResponse().withBody(jsonResponse).withStatus(200)));

		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		var feature = new Feature(new Product(productId), featureId);
		var featureUsageInfo = pmitzHttpClient.getUsageInfo(feature, userGrouping);

		assertThat(featureUsageInfo).isNotNull();
		assertThat(featureUsageInfo.featureStatus()).isEqualTo(FeatureStatus.AVAILABLE);
		var remainigUnits = featureUsageInfo.remainingUsageUnits();
		assertThat(remainigUnits).containsEntry("limit1", 10L)
				.containsEntry("limit2", 6L);
	}

	@Test
	void getUsageInfoAndRemoteResponse425ShouldThrowException() {
		//TODO complete
	}

	@Test
	void getUsageInfoAndRemoteResponseErrorShouldThrowException() {
		//TODO complete implementation
	}

	@ParameterizedTest
	@MethodSource({"userGroupingsProvider"})
	void recordOrReduceRemoteResponse200ShouldNotThrowException(UserGrouping userGrouping, String endpoint, WireMockRuntimeInfo wmRuntimeInfo) {
		String featureId = "newPicUpload";
		String productId = "picUpload";
		var feature = new Feature(new Product(productId), featureId);

		stubFor(post(endpoint + "/usage/picUpload/newPicUpload")
				.withRequestBody(equalToJson(jsonRequestBody))
				.willReturn(aResponse().withStatus(200)));

		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		pmitzHttpClient.recordOrReduce(feature, userGrouping, Map.of("limit1", 1L, "limit2", 2L), false);
	}

	@ParameterizedTest
	@MethodSource({"userGroupingsProvider"})
	void recordOrReduceRemoteResponse422ShouldThrowLimitExceededException(UserGrouping userGrouping, String endpoint, WireMockRuntimeInfo wmRuntimeInfo) {
		String featureId = "newPicUpload";
		String productId = "picUpload";
		var feature = new Feature(new Product(productId), featureId);

		stubFor(post(endpoint + "/usage/picUpload/newPicUpload")
				.withRequestBody(equalToJson(jsonRequestBody))
				.willReturn(aResponse().withStatus(422)));

		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		assertThatThrownBy(() -> pmitzHttpClient.recordOrReduce(feature, userGrouping, Map.of("limit1", 1L, "limit2", 2L), false))
				.isInstanceOf(LimitExceededException.class);
	}

	@ParameterizedTest
	@MethodSource({"userGroupingsProvider"})
	void recordOrReduceRemoteResponse4xxShouldThrowRemoteCallException(UserGrouping userGrouping, String endpoint, WireMockRuntimeInfo wmRuntimeInfo) {
		String featureId = "newPicUpload";
		String productId = "picUpload";
		var feature = new Feature(new Product(productId), featureId);

		stubFor(post(endpoint + "/usage/picUpload/newPicUpload")
				.withRequestBody(equalToJson(jsonRequestBody))
				.willReturn(aResponse().withStatus(400)));

		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		assertThatThrownBy(() -> pmitzHttpClient.recordOrReduce(feature, userGrouping, Map.of("limit1", 1L, "limit2", 2L), false))
				.isInstanceOf(FeatureNotFoundException.class);
	}

	@ParameterizedTest
	@MethodSource({"userGroupingsProvider"})
	void recordOrReduceRemoteResponse5xxShouldThrowRemoteCallException(UserGrouping userGrouping, String endpoint, WireMockRuntimeInfo wmRuntimeInfo) {
		String featureId = "newPicUpload";
		String productId = "picUpload";
		var feature = new Feature(new Product(productId), featureId);

		stubFor(post(endpoint + "/usage/picUpload/newPicUpload")
				.withRequestBody(equalToJson(jsonRequestBody))
				.willReturn(aResponse().withStatus(500)));

		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		assertThatThrownBy(() -> pmitzHttpClient.recordOrReduce(feature, userGrouping, Map.of("limit1", 1L, "limit2", 2L), false))
				.isInstanceOf(RemoteCallException.class);
	}

	@Test
	void uploadProductShouldSendPostRequest(WireMockRuntimeInfo wmRuntimeInfo) {
		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		String jsonProduct = """
				{"productId": "picshare"}
				""";

		stubFor(post("/products")
				.withRequestBody(equalToJson(jsonProduct))
				.willReturn(aResponse().withStatus(200)));

		var inputStream = new ByteArrayInputStream(jsonProduct.getBytes(StandardCharsets.UTF_8));
		pmitzHttpClient.uploadProduct(inputStream);
	}

	@Test
	void uploadProductShouldThrowExceptionWhenProductExists(WireMockRuntimeInfo wmRuntimeInfo) {
		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		String jsonProduct = """
				{"productId": "picshare"}
				""";

		stubFor(post("/products")
				.withRequestBody(equalToJson(jsonProduct))
				.willReturn(aResponse().withStatus(409)));

		var inputStream = new ByteArrayInputStream(jsonProduct.getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> pmitzHttpClient.uploadProduct(inputStream))
				.isInstanceOf(RepositoryException.class)
				.hasMessage("Product already exists");
	}

	@Test
	void removeProductShouldSendDeleteRequest(WireMockRuntimeInfo wmRuntimeInfo) {
		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		stubFor(delete("/products/aProductId")
				.willReturn(aResponse().withStatus(200)));

		pmitzHttpClient.removeProduct("aProductId");
	}

	@Test
	void removeProductWhenRemoteResponse404ShouldThrowException(WireMockRuntimeInfo wmRuntimeInfo) {
		var pmitzHttpClient = new PmitzHttpClient(wmRuntimeInfo.getHttpBaseUrl());

		stubFor(delete("/products/aProductId")
				.willReturn(aResponse().withStatus(404)));

		assertThatThrownBy(() -> pmitzHttpClient.removeProduct("aProductId"))
				.isInstanceOf(RepositoryException.class)
				.hasMessage("Product not found with id aProductId");
	}

	private static Stream<Arguments> userGroupingsProvider() {
		return Stream.of(
				Arguments.of(new Subscription("paidCustomer001"), "/subscriptions/paidCustomer001"),
				Arguments.of(new IndividualUser("user001"), "/users/user001"),
				Arguments.of(new DirectoryGroup("internalUsers"), "/directory-groups/internalUsers")
		);
	}
}
