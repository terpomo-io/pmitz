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

package io.terpomo.pmitz.all.usage.tracker.impl;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.LimitVerifier;

public class FeatureUsageTrackerImpl implements FeatureUsageTracker {

	private static final Long DEFAULT_ADDITIONAL_UNIT = 1L;

	private final LimitVerifier limitVerifier;

	private final SubscriptionVerifier subscriptionVerifier;

	public FeatureUsageTrackerImpl(LimitVerifier limitVerifier, SubscriptionVerifier subscriptionVerifier) {
		this.limitVerifier = limitVerifier;
		this.subscriptionVerifier = subscriptionVerifier;
	}

	@Override
	public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits) {
		if (!subscriptionVerifier.verifyEntitlement(feature, userGrouping).isFeatureAllowed()) {
			throw new FeatureNotAllowedException("Feature not allowed for userGrouping", feature, userGrouping);
		}
		limitVerifier.recordFeatureUsage(feature, userGrouping, requestedUnits);
	}

	@Override
	public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
		limitVerifier.reduceFeatureUsage(feature, userGrouping, reducedUnits);
	}

	@Override
	public FeatureUsageInfo verifyLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		if (!subscriptionVerifier.verifyEntitlement(feature, userGrouping).isFeatureAllowed()) {
			return new FeatureUsageInfo(FeatureStatus.NOT_ALLOWED, Collections.emptyMap());
		}

		var remainingUnits = limitVerifier.getLimitsRemainingUnits(feature, userGrouping);

		BiFunction<String, Long, Long> remainingUnitsCalculator = (key, value) -> value - additionalUnits.getOrDefault(key, DEFAULT_ADDITIONAL_UNIT);

		var remainingUnitsAfterAdditions = remainingUnits.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, mapEntry -> remainingUnitsCalculator.apply(mapEntry.getKey(), mapEntry.getValue())));

		var anyLimitExceeded = remainingUnitsAfterAdditions.entrySet().stream().anyMatch(mapEntry -> mapEntry.getValue() < 0);

		return new FeatureUsageInfo(anyLimitExceeded ? FeatureStatus.LIMIT_EXCEEDED : FeatureStatus.AVAILABLE, remainingUnitsAfterAdditions);
	}

	@Override
	public FeatureUsageInfo getUsageInfo(Feature feature, UserGrouping userGrouping) {
		if (!subscriptionVerifier.verifyEntitlement(feature, userGrouping).isFeatureAllowed()) {
			return new FeatureUsageInfo(FeatureStatus.NOT_ALLOWED, Collections.emptyMap());
		}

		var remainingUnits = limitVerifier.getLimitsRemainingUnits(feature, userGrouping);

		var anyLimitExceeded = remainingUnits.entrySet().stream().anyMatch(mapEntry -> mapEntry.getValue() < 0);

		return new FeatureUsageInfo(anyLimitExceeded ? FeatureStatus.LIMIT_EXCEEDED : FeatureStatus.AVAILABLE, remainingUnits);
	}

}
