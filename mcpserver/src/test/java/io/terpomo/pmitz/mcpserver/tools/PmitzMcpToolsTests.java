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

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
import io.terpomo.pmitz.mcpserver.backend.PmitzBackend;

import static org.assertj.core.api.Assertions.assertThat;

class PmitzMcpToolsTests {

	@Test
	void exposesDocumentedTools() {
		PmitzMcpTools tools = new PmitzMcpTools(new StubBackend());

		assertThat(tools.toolNames()).containsExactly(
				"pmitz_upload_product",
				"pmitz_remove_product",
				"pmitz_get_remaining_limits",
				"pmitz_check_limits",
				"pmitz_record_usage",
				"pmitz_reduce_usage",
				"pmitz_verify_entitlement",
				"pmitz_create_subscription",
				"pmitz_find_subscription",
				"pmitz_update_subscription_status");
		assertThat(tools.specifications()).hasSameSizeAs(tools.toolNames());
	}

	private static final class StubBackend implements PmitzBackend {

		@Override
		public void uploadProduct(InputStream inputStream) {
		}

		@Override
		public void removeProduct(String productId) {
		}

		@Override
		public FeatureUsageInfo getLimitsRemainingUnits(FeatureRef featureRef,
				UserGrouping userGrouping) {
			return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Map.of());
		}

		@Override
		public FeatureUsageInfo verifyLimits(FeatureRef featureRef, UserGrouping userGrouping,
				Map<String, Long> additionalUnits) {
			return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Map.of());
		}

		@Override
		public void recordUsage(FeatureRef featureRef, UserGrouping userGrouping,
				Map<String, Long> units) {
		}

		@Override
		public void reduceUsage(FeatureRef featureRef, UserGrouping userGrouping,
				Map<String, Long> units) {
		}

		@Override
		public SubscriptionVerifDetail verifySubscription(FeatureRef featureRef,
				UserGrouping userGrouping) {
			return SubscriptionVerifDetail.verificationOk();
		}

		@Override
		public void createSubscription(Subscription subscription) {
		}

		@Override
		public Optional<Subscription> findSubscription(String subscriptionId) {
			return Optional.empty();
		}

		@Override
		public void updateSubscriptionStatus(String subscriptionId, SubscriptionStatus status) {
		}

	}

}
