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

package io.terpomo.pmitz.mcpserver.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class ProductJsonInput {

	private ProductJsonInput() {
	}

	static InputStream normalize(InputStream inputStream) {
		try {
			String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			String trimmed = json.stripLeading();
			if (trimmed.startsWith("{")) {
				return new ByteArrayInputStream(("[" + json + "]").getBytes(StandardCharsets.UTF_8));
			}
			return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to read product JSON", ex);
		}
	}

}
