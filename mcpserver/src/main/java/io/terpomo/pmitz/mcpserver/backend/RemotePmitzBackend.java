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

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.remote.client.PmitzClient;

public final class RemotePmitzBackend implements PmitzBackend {

	private final PmitzClient client;

	public RemotePmitzBackend(PmitzClient client) {
		this.client = client;
	}

	@Override
	public void uploadProduct(InputStream inputStream) {
		client.uploadProduct(inputStream);
	}

	@Override
	public void removeProduct(String productId) {
		client.removeProduct(productId);
	}

	@Override
	public FeatureUsageInfo getLimitsRemainingUnits(FeatureRef featureRef, UserGrouping userGrouping) {
		return client.getLimitsRemainingUnits(featureRef, userGrouping);
	}

	@Override
	public FeatureUsageInfo verifyLimits(FeatureRef featureRef, UserGrouping userGrouping,
			Map<String, Long> additionalUnits) {
		return client.verifyLimits(featureRef, userGrouping, additionalUnits);
	}

	@Override
	public void recordUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> units) {
		client.recordOrReduce(featureRef, userGrouping, units, false);
	}

	@Override
	public void reduceUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> units) {
		client.recordOrReduce(featureRef, userGrouping, units, true);
	}

	@Override
	public SubscriptionVerifDetail verifySubscription(FeatureRef featureRef, UserGrouping userGrouping) {
		return client.verifySubscription(featureRef, userGrouping);
	}

	@Override
	public void createSubscription(Subscription subscription) {
		client.createSubscription(subscription);
	}

	@Override
	public Optional<Subscription> findSubscription(String subscriptionId) {
		return client.findSubscription(subscriptionId);
	}

	@Override
	public void updateSubscriptionStatus(String subscriptionId, SubscriptionStatus status) {
		client.updateSubscriptionStatus(subscriptionId, status);
	}

}
