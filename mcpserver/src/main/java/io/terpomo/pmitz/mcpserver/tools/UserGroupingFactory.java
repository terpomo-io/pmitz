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

import java.util.Locale;

import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;

final class UserGroupingFactory {

	private UserGroupingFactory() {
	}

	static UserGrouping create(String type, String id) {
		return switch (type.trim().toLowerCase(Locale.ROOT)) {
			case "user", "users", "individual-user" -> new IndividualUser(id);
			case "directory-group", "directory-groups", "group" -> new DirectoryGroup(id);
			case "subscription", "subscriptions" -> new Subscription(id);
			default -> throw new IllegalArgumentException("Unsupported subjectType: " + type);
		};
	}

}
