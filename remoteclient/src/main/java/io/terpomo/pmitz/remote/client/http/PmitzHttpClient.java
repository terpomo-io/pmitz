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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotFoundException;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.limits.impl.LimitsValidationUtil;
import io.terpomo.pmitz.remote.client.PmitzClient;
import io.terpomo.pmitz.remote.client.RemoteCallException;


public class PmitzHttpClient implements PmitzClient {

	public static final String URL_DELIMITER = "/";
	private final String url;

	private final CloseableHttpClient httpClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<Class<?>, String> userGroupingTypes;
	private final PmitzHttpAuthProvider authProvider;

	public PmitzHttpClient(String url, PmitzHttpAuthProvider authProvider) {
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		this.url = url;

		httpClient = HttpClients.createDefault();

		this.authProvider = authProvider;

		userGroupingTypes = Map.of(IndividualUser.class, "users",
				DirectoryGroup.class, "directory-groups",
				Subscription.class, "subscriptions");
	}

	@Override
	public FeatureUsageInfo getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
		HttpGet httpGet = new HttpGet(url + URL_DELIMITER + formatEndpoint("usage", userGrouping, feature));
		JsonNode responseData;
		addAuthenticationHeaders(httpGet);
		try {
			responseData = httpClient.execute(httpGet, response -> {
				if (response.getCode() >= 400 && response.getCode() < 500) {
					throw new FeatureNotFoundException("Invalid productId or FeatureId : " + response.getReasonPhrase());
				}
				else if (response.getCode() >= 300) {
					throw new RemoteCallException(response.getReasonPhrase());
				}
				final HttpEntity responseEntity = response.getEntity();
				if (responseEntity == null) {
					throw new RemoteCallException("Unexpected response from server (response empty)");
				}
				try (InputStream inputStream = responseEntity.getContent()) {
					return objectMapper.readTree(inputStream);
				}
			});
		}
		catch (IOException ioEx) {
			throw new RemoteCallException("Unexpected error while calling remote server", ioEx);
		}

		try {
			return objectMapper.treeToValue(responseData, FeatureUsageInfo.class);
		}
		catch (JsonProcessingException jsonEx) {
			throw new RemoteCallException("Unexpected error while parsing server response", jsonEx);
		}
	}

	@Override
	public FeatureUsageInfo verifyLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		HttpPost httpPost = new HttpPost(url + URL_DELIMITER + formatEndpoint("limits-check", userGrouping, feature));

		try {
			var jsonBody = objectMapper.writeValueAsString(additionalUnits);
			httpPost.setEntity(new StringEntity(jsonBody));
			httpPost.setHeader("Content-Type", "application/json");
			addAuthenticationHeaders(httpPost);
		}
		catch (JsonProcessingException jsonEx) {
			throw new RemoteCallException("Unexpected exception while preparing request", jsonEx);
		}

		JsonNode responseData;
		try {
			responseData = httpClient.execute(httpPost, response -> {
				if (response.getCode() >= 400 && response.getCode() < 500) {
					throw new FeatureNotFoundException("Invalid productId or FeatureId : " + response.getReasonPhrase());
				}
				else if (response.getCode() >= 300) {
					throw new RemoteCallException(response.getReasonPhrase());
				}
				final HttpEntity responseEntity = response.getEntity();
				if (responseEntity == null) {
					throw new RemoteCallException("Unexpected response from server (response empty)");
				}
				try (InputStream inputStream = responseEntity.getContent()) {
					return objectMapper.readTree(inputStream);
				}
			});
		}
		catch (IOException ioEx) {
			throw new RemoteCallException("Unexpected error while calling remote server", ioEx);
		}

		try {
			return objectMapper.treeToValue(responseData, FeatureUsageInfo.class);
		}
		catch (JsonProcessingException jsonEx) {
			throw new RemoteCallException("Unexpected error while parsing server response", jsonEx);
		}
	}

	@Override
	public void recordOrReduce(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits, boolean isReduce) {
		LimitsValidationUtil.validateAdditionalUnits(additionalUnits);
		HttpPost httpPost = new HttpPost(url + URL_DELIMITER + formatEndpoint("usage", userGrouping, feature));

		var recordOrReduceRequest = new RecordOrReduceRequest(isReduce, additionalUnits);
		try {
			var jsonBody = objectMapper.writeValueAsString(recordOrReduceRequest);
			httpPost.setEntity(new StringEntity(jsonBody));
			httpPost.setHeader("Content-Type", "application/json");
			addAuthenticationHeaders(httpPost);
		}
		catch (JsonProcessingException jsonEx) {
			throw new RemoteCallException("Unexpected exception while preparing request", jsonEx);
		}

		try {
			httpClient.execute(httpPost, response -> {
				if (response.getCode() == 422) {
					throw new LimitExceededException("Limit exceeded", feature, userGrouping);
				}
				else if (response.getCode() >= 400 && response.getCode() < 500) {
					throw new FeatureNotFoundException("Invalid productId or FeatureId : " + response.getReasonPhrase());
				}
				else if (response.getCode() >= 300) {
					throw new RemoteCallException(response.getReasonPhrase());
				}
				return null;
			});
		}
		catch (IOException ioEx) {
			throw new RemoteCallException("Unexpected error while calling remote server", ioEx);
		}
	}

	@Override
	public void uploadProduct(InputStream inputStream) {
		HttpPost httpPost = new HttpPost(url + URL_DELIMITER + "products");
		httpPost.setEntity(new InputStreamEntity(inputStream, ContentType.APPLICATION_JSON));
		addAuthenticationHeaders(httpPost);
		try {
			httpClient.execute(httpPost, response -> {
				if (response.getCode() == 409) {
					throw new RepositoryException("Product already exists");
				}
				if (response.getCode() >= 400) {
					throw new RemoteCallException("Error encountered while uploading product");
				}
				return null;
			});
		}
		catch (IOException ioEx) {
			throw new RemoteCallException("Unexpected error while calling remote server", ioEx);
		}
	}

	@Override
	public void removeProduct(String productId) {
		HttpDelete httpDelete = new HttpDelete(url + URL_DELIMITER + "products" + URL_DELIMITER + productId);
		try {
			addAuthenticationHeaders(httpDelete);
			httpClient.execute(httpDelete, response -> {
				if (response.getCode() == 404) {
					throw new RepositoryException("Product not found with id " + productId);
				}
				else if (response.getCode() >= 400) {
					throw new RemoteCallException("Error encountered while removing product");
				}
				return null;
			});
		}
		catch (IOException ioEx) {
			throw new RemoteCallException("Unexpected error while calling remote server", ioEx);
		}
	}

	private String formatEndpoint(String resource, UserGrouping userGrouping, Feature feature) {
		String rootEndpoint = userGroupingTypes.get(userGrouping.getClass());
		String productId = feature.getProduct().getProductId();
		String featureId = feature.getFeatureId();
		return String.join(URL_DELIMITER, rootEndpoint, userGrouping.getId(), resource, productId, featureId);
	}

	private void addAuthenticationHeaders(HttpUriRequestBase httpUriRequestBase){
		authProvider.getAuthenticationHeaders().entrySet()
				.forEach(entry -> httpUriRequestBase.setHeader(entry.getKey(), entry.getValue()));
	}

}
