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

package io.terpomo.pmitz.core.subscriptions;

import java.time.ZonedDateTime;

import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public class Subscription extends UserGrouping {

	private String subscriptionId;
	private SubscriptionStatus status;
	private ZonedDateTime expirationDate;


	private Plan plan;

	public Subscription(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public boolean isActive() {
		return SubscriptionStatus.ACTIVE == status;
	}

	public boolean isExpired() {
		return SubscriptionStatus.EXPIRED == status;
	}

	@Override
	public String getId() {
		return subscriptionId;
	}

	public Plan getPlan() {
		return plan;
	}
}
