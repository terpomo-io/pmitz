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

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.limits.LimitVerifierBuilder;
import io.terpomo.pmitz.mcpserver.backend.LocalPmitzBackend;
import io.terpomo.pmitz.subscriptions.SubscriptionVerifierBuilder;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

import static org.assertj.core.api.Assertions.assertThat;

class PmitzMcpLocalE2ETests {

	@Test
	void executesLocalPmitzWorkflowThroughMcpTools() throws Exception {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl("jdbc:h2:mem:pmitz_mcp_e2e;DB_CLOSE_DELAY=-1");
		dataSource.setUsername("sa");
		initSchema(dataSource);

		var productRepository = new InMemoryProductRepository();
		var limitVerifier = LimitVerifierBuilder.of(productRepository)
				.withDefaultLimitRuleResolver()
				.withJdbcUsageRepository(dataSource, "dbo", "usage")
				.build();
		var subscriptionRepository = new JDBCSubscriptionRepository(dataSource, "dbo",
				"subscription", "subscription_plan");
		var subscriptionVerifier = SubscriptionVerifierBuilder
				.withSubscriptionRepository(subscriptionRepository)
				.withDefaultSubscriptionFeatureManager(productRepository)
				.build();
		var tools = new PmitzMcpTools(new LocalPmitzBackend(productRepository, limitVerifier,
				subscriptionVerifier, subscriptionRepository));

		call(tools, "pmitz_upload_product", Map.of("productJson", productJson()));
		call(tools, "pmitz_create_subscription", Map.of(
				"subscriptionId", "sub-e2e",
				"status", SubscriptionStatus.ACTIVE.name(),
				"plansByProduct", Map.of("Library", "Basic")));
		CallToolResult subscription = call(tools, "pmitz_find_subscription",
				Map.of("subscriptionId", "sub-e2e"));
		assertThat(subscription.content().get(0).toString()).contains("sub-e2e");

		CallToolResult entitlement = call(tools, "pmitz_verify_entitlement", Map.of(
				"productId", "Library",
				"featureId", "Books",
				"subjectType", "subscription",
				"subjectId", "sub-e2e"));
		assertThat(entitlement.content().get(0).toString()).contains("featureAllowed\":true");

		call(tools, "pmitz_record_usage", Map.of(
				"productId", "Library",
				"featureId", "Books",
				"subjectType", "user",
				"subjectId", "user-e2e",
				"units", Map.of("Max books", 2L)));
		CallToolResult remaining = call(tools, "pmitz_get_remaining_limits", Map.of(
				"productId", "Library",
				"featureId", "Books",
				"subjectType", "user",
				"subjectId", "user-e2e"));
		assertThat(remaining.content().get(0).toString()).contains("Max books").contains("3");

		CallToolResult check = call(tools, "pmitz_check_limits", Map.of(
				"productId", "Library",
				"featureId", "Books",
				"subjectType", "user",
				"subjectId", "user-e2e",
				"units", Map.of("Max books", 3L)));
		assertThat(check.content().get(0).toString()).contains("AVAILABLE");

		call(tools, "pmitz_reduce_usage", Map.of(
				"productId", "Library",
				"featureId", "Books",
				"subjectType", "user",
				"subjectId", "user-e2e",
				"units", Map.of("Max books", 1L)));
		call(tools, "pmitz_update_subscription_status", Map.of(
				"subscriptionId", "sub-e2e",
				"status", SubscriptionStatus.SUSPENDED.name()));
		call(tools, "pmitz_remove_product", Map.of("productId", "Library"));
	}

	private CallToolResult call(PmitzMcpTools tools, String name, Map<String, Object> arguments) {
		SyncToolSpecification specification = tools.specifications()
				.stream()
				.filter(tool -> tool.tool().name().equals(name))
				.findFirst()
				.orElseThrow();
		CallToolResult result = specification.callHandler()
				.apply(null, new CallToolRequest(name, arguments));
		assertThat(result.isError()).as(name).isNotEqualTo(Boolean.TRUE);
		return result;
	}

	private void initSchema(BasicDataSource dataSource) throws Exception {
		String sql = """
				create schema if not exists dbo;
				CREATE TABLE dbo.usage (
				    usage_id IDENTITY,
				    feature_id VARCHAR(255),
				    product_id VARCHAR(255),
				    user_grouping VARCHAR(255),
				    limit_id VARCHAR(255),
				    window_start TIMESTAMP,
				    window_end TIMESTAMP,
				    units INT,
				    expiration_date TIMESTAMP,
				    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
				    PRIMARY KEY (usage_id)
				);
				CREATE TABLE dbo.subscription (
				    subscription_id VARCHAR(255) PRIMARY KEY,
				    status VARCHAR(50) NOT NULL,
				    expiration_date TIMESTAMP
				);
				CREATE TABLE dbo.subscription_plan (
				    subscription_id VARCHAR(255) NOT NULL,
				    product_id VARCHAR(255) NOT NULL,
				    plan_id VARCHAR(255) NOT NULL,
				    PRIMARY KEY (subscription_id, product_id),
				    CONSTRAINT fk_subscription_plan_subscription FOREIGN KEY (subscription_id)
				        REFERENCES dbo.subscription(subscription_id)
				);
				""";
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			for (String command : sql.split(";")) {
				if (!command.isBlank()) {
					statement.execute(command);
				}
			}
		}
	}

	private String productJson() {
		return """
				{
				  "productId": "Library",
				  "features": [{
				    "featureId": "Books",
				    "limits": [{
				      "type": "CountLimit",
				      "id": "Max books",
				      "count": 5
				    }]
				  }],
				  "plans": [{
				    "planId": "Basic",
				    "includedFeatures": ["Books"]
				  }]
				}
				""";
	}

}
