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

import io.terpomo.pmitz.all.usage.tracker.impl.FeatureUsageTrackerImpl;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.LimitVerifier;

public final class LocalPmitzBackend implements PmitzBackend {

	private final InMemoryProductRepository productRepository;

	private final LimitVerifier limitVerifier;

	private final SubscriptionVerifier subscriptionVerifier;

	private final SubscriptionRepository subscriptionRepository;

	private final FeatureUsageTrackerImpl usageTracker;

	public LocalPmitzBackend(InMemoryProductRepository productRepository, LimitVerifier limitVerifier,
			SubscriptionVerifier subscriptionVerifier, SubscriptionRepository subscriptionRepository) {
		this.productRepository = productRepository;
		this.limitVerifier = limitVerifier;
		this.subscriptionVerifier = subscriptionVerifier;
		this.subscriptionRepository = subscriptionRepository;
		this.usageTracker = new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
	}

	@Override
	public void uploadProduct(InputStream inputStream) {
		productRepository.load(ProductJsonInput.normalize(inputStream));
	}

	@Override
	public void removeProduct(String productId) {
		Product product = productRepository.getProductById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
		productRepository.removeProduct(product);
	}

	@Override
	public FeatureUsageInfo getLimitsRemainingUnits(FeatureRef featureRef, UserGrouping userGrouping) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE,
				limitVerifier.getLimitsRemainingUnits(featureRef, userGrouping));
	}

	@Override
	public FeatureUsageInfo verifyLimits(FeatureRef featureRef, UserGrouping userGrouping,
			Map<String, Long> additionalUnits) {
		return usageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);
	}

	@Override
	public void recordUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> units) {
		usageTracker.recordFeatureUsage(featureRef, userGrouping, units);
	}

	@Override
	public void reduceUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> units) {
		usageTracker.reduceFeatureUsage(featureRef, userGrouping, units);
	}

	@Override
	public SubscriptionVerifDetail verifySubscription(FeatureRef featureRef, UserGrouping userGrouping) {
		return subscriptionVerifier.verifyEntitlement(featureRef, userGrouping);
	}

	@Override
	public void createSubscription(Subscription subscription) {
		subscriptionRepository.create(subscription);
	}

	@Override
	public Optional<Subscription> findSubscription(String subscriptionId) {
		return subscriptionRepository.find(subscriptionId);
	}

	@Override
	public void updateSubscriptionStatus(String subscriptionId, SubscriptionStatus status) {
		subscriptionRepository.updateStatus(subscriptionId, status);
	}

}
