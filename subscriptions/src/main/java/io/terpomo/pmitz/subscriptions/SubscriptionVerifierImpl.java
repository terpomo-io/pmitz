/*
 * Copyright 2023-2025 the original author or authors.
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

package io.terpomo.pmitz.subscriptions;

import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;

public class SubscriptionVerifierImpl implements SubscriptionVerifier {

	private io.terpomo.pmitz.core.subscriptions.SubscriptionRepository subscriptionRepository;
	private SubscriptionFeatureManager subscriptionFeatureManager;

	public SubscriptionVerifierImpl(SubscriptionRepository subscriptionRepository, SubscriptionFeatureManager subscriptionFeatureManager) {
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionFeatureManager = subscriptionFeatureManager;
	}

	@Override
	public SubscriptionVerifDetail verifyEntitlement(Feature feature, UserGrouping userGrouping) {
		if (!(userGrouping instanceof Subscription)) {
			return SubscriptionVerifDetail.verificationOk();
		}

		String productId = feature.getProduct().getProductId();
		Optional<Subscription> optSubscription = subscriptionRepository.find(userGrouping.getId());

		SubscriptionVerifDetail.ErrorCause errorCause = null;

		if (optSubscription.isEmpty() || !optSubscription.get().isValid()) {
			errorCause = SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION;
		}
		else if (!optSubscription.get().isProductAllowed(productId)) {
			errorCause = SubscriptionVerifDetail.ErrorCause.PRODUCT_NOT_ALLOWED;
		}
		else if (!subscriptionFeatureManager.isFeatureIncluded(optSubscription.get(), feature)) {
			errorCause = SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED;
		}

		return (errorCause != null) ? SubscriptionVerifDetail.verificationError(errorCause) : SubscriptionVerifDetail.verificationOk();

	}

}
