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

package io.terpomo.pmitz.remote.client;

import java.util.Optional;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.remote.client.http.PmitzApiKeyAuthenticationProvider;
import io.terpomo.pmitz.remote.client.http.PmitzHttpClient;

public class SubscriptionRepoRemoteClient implements SubscriptionRepository {

	private final PmitzClient pmitzClient;

	public SubscriptionRepoRemoteClient(String url) {
		this(new PmitzHttpClient(url, new PmitzApiKeyAuthenticationProvider()));
	}

	public SubscriptionRepoRemoteClient(PmitzClient pmitzClient) {
		this.pmitzClient = pmitzClient;
	}

	@Override
	public void create(Subscription subscription) {
		pmitzClient.createSubscription(subscription);
	}

	@Override
	public Optional<Subscription> find(String subscriptionId) {
		return pmitzClient.findSubscription(subscriptionId);
	}

	@Override
	public void updateStatus(String subscriptionId, SubscriptionStatus newStatus) {
		pmitzClient.updateSubscriptionStatus(subscriptionId, newStatus);
	}
}
