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

package io.terpomo.pmitz.limits.impl.strategy;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.limits.LimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;

public class SimpleLimitVerificationStrategy implements LimitVerificationStrategy {

	@Override
	public void recordFeatureUsage(LimitTrackingContext context, LimitRule limitRule, long additionalUnits) {
		var now = ZonedDateTime.now();
		var optExistingUsageRecord = findCurrentUsage(context, limitRule, now);
		long alreadyUsedUnits = optExistingUsageRecord.isEmpty() ? 0 : optExistingUsageRecord.get().units();
		long newUnits = alreadyUsedUnits + additionalUnits;

		if (newUnits > limitRule.getValue()) {
			throw new LimitExceededException("Limit will be exceeded if additional units are used. " + alreadyUsedUnits + " are currently used, out of " + limitRule.getValue() + ".",
					context.getFeatureRef(), context.getUserGrouping());
		}

		var windowEnd = getWindowEnd(limitRule, now).orElse(null);
		var updatedRecord = optExistingUsageRecord.map(usageRecord -> UsageRecord.updage(usageRecord, newUnits, usageRecord.expirationDate()))
				.orElseGet(() -> new UsageRecord(limitRule.getId(), getWindowStart(limitRule, now).orElse(null), windowEnd, newUnits, calculateExpirationDate(windowEnd)));

		context.addUpdatedUsageRecords(Collections.singletonList(updatedRecord));
	}

	@Override
	public void reduceFeatureUsage(LimitTrackingContext context, LimitRule limitRule, long reducedUnits) {
		var now = ZonedDateTime.now();
		var optExistingUsageRecord = findCurrentUsage(context, limitRule, now);

		var alreadyUsedUnits = optExistingUsageRecord.isEmpty() ? 0 : optExistingUsageRecord.get().units();

		var newUnits = (alreadyUsedUnits > reducedUnits) ? alreadyUsedUnits - reducedUnits : 0;

		var windowEnd = getWindowEnd(limitRule, now).orElse(null);
		var updatedRecord = optExistingUsageRecord.map(usageRecord -> UsageRecord.updage(usageRecord, newUnits, usageRecord.expirationDate()))
				.orElseGet(() -> new UsageRecord(limitRule.getId(), getWindowStart(limitRule, now).orElse(null), windowEnd, newUnits, calculateExpirationDate(windowEnd)));
		context.addUpdatedUsageRecords(Collections.singletonList(updatedRecord));
	}

	@Override
	public boolean isWithinLimits(LimitTrackingContext context, LimitRule limitRule, long additionalUnits) {
		return getRemainingUnits(context, limitRule) >= additionalUnits;
	}

	@Override
	public long getRemainingUnits(LimitTrackingContext context, LimitRule limitRule) {
		var now = ZonedDateTime.now();
		var optUsageRecord = findCurrentUsage(context, limitRule, now);
		long currentUsage = optUsageRecord.isEmpty() ? 0 : optUsageRecord.get().units();
		return limitRule.getValue() - currentUsage;
	}

	@Override
	public Optional<ZonedDateTime> getWindowStart(LimitRule limitRule, ZonedDateTime referenceDate) {
		return limitRule.getWindowStart(referenceDate);
	}

	@Override
	public Optional<ZonedDateTime> getWindowEnd(LimitRule limitRule, ZonedDateTime referenceDate) {
		return limitRule.getWindowEnd(referenceDate);
	}

	private ZonedDateTime calculateExpirationDate(ZonedDateTime windowEnd) {
		return (windowEnd != null) ? windowEnd.plusMonths(3) : null;
	}

	private Optional<UsageRecord> findCurrentUsage(LimitTrackingContext context, LimitRule limitRule, ZonedDateTime referenceDate) {
		var usageRecordList = context.findUsageRecords(limitRule.getId(), getWindowStart(limitRule, referenceDate).orElse(null), getWindowEnd(limitRule, referenceDate).orElse(null));

		if (usageRecordList.size() > 1) {
			throw new IllegalStateException("Inconsistent data found in usage repository. Should find 1 record atmost for this type of limit");
		}

		return (usageRecordList.isEmpty() ? Optional.empty() : Optional.of(usageRecordList.get(0)));
	}
}
