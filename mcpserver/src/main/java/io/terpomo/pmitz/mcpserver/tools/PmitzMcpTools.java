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

package io.terpomo.pmitz.mcpserver.tools;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.mcpserver.backend.PmitzBackend;

public final class PmitzMcpTools {

	private static final String PRODUCT_ID = "productId";

	private static final String FEATURE_ID = "featureId";

	private static final String SUBJECT_TYPE = "subjectType";

	private static final String SUBJECT_ID = "subjectId";

	private final PmitzBackend backend;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	public PmitzMcpTools(PmitzBackend backend) {
		this.backend = backend;
	}

	public List<String> toolNames() {
		return List.of("pmitz_upload_product", "pmitz_remove_product", "pmitz_get_remaining_limits",
				"pmitz_check_limits", "pmitz_record_usage", "pmitz_reduce_usage",
				"pmitz_verify_entitlement", "pmitz_create_subscription", "pmitz_find_subscription",
				"pmitz_update_subscription_status");
	}

	public List<SyncToolSpecification> specifications() {
		return List.of(uploadProduct(), removeProduct(), getRemainingLimits(), checkLimits(), recordUsage(),
				reduceUsage(), verifyEntitlement(), createSubscription(), findSubscription(),
				updateSubscriptionStatus());
	}

	private SyncToolSpecification uploadProduct() {
		return tool("pmitz_upload_product", "Load or replace Pmitz product definitions from JSON.",
				schema(Map.of("productJson", string("Product JSON array or object accepted by Pmitz.")),
						List.of("productJson")),
				request -> {
					String productJson = requiredString(request, "productJson");
					backend.uploadProduct(new ByteArrayInputStream(productJson.getBytes(StandardCharsets.UTF_8)));
					return ok(Map.of("uploaded", true));
				});
	}

	private SyncToolSpecification removeProduct() {
		return tool("pmitz_remove_product", "Remove a product definition by product id.",
				schema(Map.of(PRODUCT_ID, string("Product id.")), List.of(PRODUCT_ID)),
				request -> {
					backend.removeProduct(requiredString(request, PRODUCT_ID));
					return ok(Map.of("removed", true));
				});
	}

	private SyncToolSpecification getRemainingLimits() {
		return featureSubjectTool("pmitz_get_remaining_limits",
				"Get remaining usage units for a feature and user grouping.",
				request -> ok(backend.getLimitsRemainingUnits(featureRef(request), userGrouping(request))));
	}

	private SyncToolSpecification checkLimits() {
		return featureSubjectUnitsTool("pmitz_check_limits",
				"Check whether requested usage units fit inside current limits.",
				request -> ok(backend.verifyLimits(featureRef(request), userGrouping(request), units(request))));
	}

	private SyncToolSpecification recordUsage() {
		return featureSubjectUnitsTool("pmitz_record_usage",
				"Record feature usage after checking subscription entitlement and limits.",
				request -> {
					backend.recordUsage(featureRef(request), userGrouping(request), units(request));
					return ok(Map.of("recorded", true));
				});
	}

	private SyncToolSpecification reduceUsage() {
		return featureSubjectUnitsTool("pmitz_reduce_usage",
				"Reduce recorded usage units, for example after an action is undone.",
				request -> {
					backend.reduceUsage(featureRef(request), userGrouping(request), units(request));
					return ok(Map.of("reduced", true));
				});
	}

	private SyncToolSpecification verifyEntitlement() {
		return featureSubjectTool("pmitz_verify_entitlement",
				"Verify whether a user grouping is entitled to a product feature.",
				request -> ok(backend.verifySubscription(featureRef(request), userGrouping(request))));
	}

	private SyncToolSpecification createSubscription() {
		return tool("pmitz_create_subscription", "Create a subscription with status, optional expiration, and plans.",
				schema(Map.of(
						"subscriptionId", string("Subscription id."),
						"status", string("Subscription status such as ACTIVE, SUSPENDED, or CANCELLED."),
						"expirationDate", string("Optional ISO-8601 zoned date time."),
						"plansByProduct", object("Map of product id to plan id.")),
						List.of("subscriptionId", "status")),
				request -> {
					Subscription subscription = new Subscription(requiredString(request, "subscriptionId"));
					subscription.setStatus(SubscriptionStatus.valueOf(requiredString(request, "status")));
					String expirationDate = optionalString(request, "expirationDate");
					if (expirationDate != null) {
						subscription.setExpirationDate(ZonedDateTime.parse(expirationDate));
					}
					subscription.setPlans(stringMap(request.arguments().get("plansByProduct")));
					backend.createSubscription(subscription);
					return ok(Map.of("created", true));
				});
	}

	private SyncToolSpecification findSubscription() {
		return tool("pmitz_find_subscription", "Find a subscription by id.",
				schema(Map.of("subscriptionId", string("Subscription id.")), List.of("subscriptionId")),
				request -> ok(Map.of("subscription",
						backend.findSubscription(requiredString(request, "subscriptionId")).orElse(null))));
	}

	private SyncToolSpecification updateSubscriptionStatus() {
		return tool("pmitz_update_subscription_status", "Update a subscription status.",
				schema(Map.of(
						"subscriptionId", string("Subscription id."),
						"status", string("New subscription status.")),
						List.of("subscriptionId", "status")),
				request -> {
					backend.updateSubscriptionStatus(requiredString(request, "subscriptionId"),
							SubscriptionStatus.valueOf(requiredString(request, "status")));
					return ok(Map.of("updated", true));
				});
	}

	private SyncToolSpecification featureSubjectTool(String name, String description, ToolHandler handler) {
		return tool(name, description,
				schema(featureSubjectProperties(), List.of(PRODUCT_ID, FEATURE_ID, SUBJECT_TYPE, SUBJECT_ID)),
				handler);
	}

	private SyncToolSpecification featureSubjectUnitsTool(String name, String description,
			ToolHandler handler) {
		Map<String, Object> properties = new java.util.LinkedHashMap<>(featureSubjectProperties());
		properties.put("units", object("Map of limit id to requested unit count."));
		return tool(name, description,
				schema(properties, List.of(PRODUCT_ID, FEATURE_ID, SUBJECT_TYPE, SUBJECT_ID, "units")),
				handler);
	}

	private Map<String, Object> featureSubjectProperties() {
		return Map.of(PRODUCT_ID, string("Product id."), FEATURE_ID, string("Feature id."),
				SUBJECT_TYPE, string("One of user, directory-group, subscription."),
				SUBJECT_ID, string("User grouping id."));
	}

	private SyncToolSpecification tool(String name, String description, JsonSchema schema,
			ToolHandler handler) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder().name(name).description(description).inputSchema(schema).build())
				.callHandler((exchange, request) -> call(handler, request))
				.build();
	}

	private CallToolResult call(ToolHandler handler, CallToolRequest request) {
		try {
			return handler.handle(request);
		}
		catch (RuntimeException ex) {
			return CallToolResult.builder()
					.isError(true)
					.addTextContent(ex.getMessage())
					.structuredContent(Map.of("error", ex.getClass().getSimpleName(),
							"message", ex.getMessage()))
					.build();
		}
	}

	private CallToolResult ok(Object value) {
		try {
			String json = objectMapper.writeValueAsString(value);
			return CallToolResult.builder()
					.addTextContent(json)
					.structuredContent(value)
					.build();
		}
		catch (JacksonException ex) {
			throw new IllegalArgumentException("Unable to serialize tool result", ex);
		}
	}

	private FeatureRef featureRef(CallToolRequest request) {
		return new FeatureRef(requiredString(request, PRODUCT_ID), requiredString(request, FEATURE_ID));
	}

	private UserGrouping userGrouping(CallToolRequest request) {
		return UserGroupingFactory.create(requiredString(request, SUBJECT_TYPE), requiredString(request, SUBJECT_ID));
	}

	private Map<String, Long> units(CallToolRequest request) {
		return longMap(request.arguments().get("units"));
	}

	private JsonSchema schema(Map<String, Object> properties, List<String> required) {
		return new JsonSchema("object", properties, required, false, null, null);
	}

	private Map<String, Object> string(String description) {
		return Map.of("type", "string", "description", description);
	}

	private Map<String, Object> object(String description) {
		return Map.of("type", "object", "description", description);
	}

	private String requiredString(CallToolRequest request, String name) {
		String value = optionalString(request, name);
		if (value == null) {
			throw new IllegalArgumentException("Missing required argument: " + name);
		}
		return value;
	}

	private String optionalString(CallToolRequest request, String name) {
		Object value = request.arguments().get(name);
		return (value != null && !value.toString().isBlank()) ? value.toString() : null;
	}

	private Map<String, Long> longMap(Object value) {
		if (value == null) {
			return Map.of();
		}
		try {
			return objectMapper.convertValue(value, objectMapper.getTypeFactory()
					.constructMapType(Map.class, String.class, Long.class));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Expected object with string keys and long values", ex);
		}
	}

	private Map<String, String> stringMap(Object value) {
		if (value == null) {
			return Map.of();
		}
		try {
			return objectMapper.convertValue(value, objectMapper.getTypeFactory()
					.constructMapType(Map.class, String.class, String.class));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Expected object with string keys and string values", ex);
		}
	}

	private interface ToolHandler {

		CallToolResult handle(CallToolRequest request);

	}

}
