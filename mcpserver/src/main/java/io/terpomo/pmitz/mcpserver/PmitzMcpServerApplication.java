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

package io.terpomo.pmitz.mcpserver;

import java.util.Arrays;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

import io.terpomo.pmitz.mcpserver.backend.PmitzBackend;
import io.terpomo.pmitz.mcpserver.backend.PmitzBackendFactory;
import io.terpomo.pmitz.mcpserver.backend.ToolListingPmitzBackend;
import io.terpomo.pmitz.mcpserver.config.PmitzMcpConfig;
import io.terpomo.pmitz.mcpserver.tools.PmitzMcpTools;

public final class PmitzMcpServerApplication {

	private PmitzMcpServerApplication() {
	}

	public static void main(String[] args) {
		if (Arrays.asList(args).contains("--help")) {
			printHelp();
			return;
		}
		if (Arrays.asList(args).contains("--list-tools")) {
			new PmitzMcpTools(new ToolListingPmitzBackend()).toolNames().forEach(System.out::println);
			return;
		}

		PmitzMcpConfig config = PmitzMcpConfig.fromEnvironment();
		PmitzBackend backend = PmitzBackendFactory.create(config);
		PmitzMcpTools tools = new PmitzMcpTools(backend);

		var jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
		var transportProvider = new StdioServerTransportProvider(jsonMapper);

		McpServer.sync(transportProvider)
				.serverInfo("pmitz-mcpserver", config.version())
				.capabilities(ServerCapabilities.builder().tools(true).build())
				.tools(tools.specifications())
				.build();
	}

	private static void printHelp() {
		System.out.println("""
				Pmitz MCP Server

				Usage:
				  ./gradlew :mcpserver:run
				  ./gradlew :mcpserver:run --args='--list-tools'

				Configuration:
				  PMITZ_MCP_MODE=remote|local

				Remote mode:
				  PMITZ_REMOTE_URL=http://localhost:8080
				  PMITZ_API_KEY=<api key consumed by remoteclient>

				Local mode:
				  PMITZ_PRODUCT_FILE=/path/to/products.json
				  PMITZ_JDBC_URL=jdbc:h2:mem:pmitz
				  PMITZ_JDBC_USERNAME=sa
				  PMITZ_JDBC_PASSWORD=
				  PMITZ_DB_SCHEMA=PUBLIC
				  PMITZ_USAGE_TABLE=usage
				  PMITZ_SUBSCRIPTION_TABLE=subscription
				  PMITZ_SUBSCRIPTION_PLAN_TABLE=subscription_plan
				""");
	}

}
