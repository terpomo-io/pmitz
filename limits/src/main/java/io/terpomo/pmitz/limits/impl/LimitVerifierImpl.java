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

package io.terpomo.pmitz.limits.impl;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.LimitRuleResolver;
import io.terpomo.pmitz.limits.LimitVerificationStrategy;
import io.terpomo.pmitz.limits.LimitVerificationStrategyResolver;
import io.terpomo.pmitz.limits.LimitVerifier;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

public class LimitVerifierImpl implements LimitVerifier {
	private final LimitRuleResolver limitRuleResolver;

	private final LimitVerificationStrategyResolver limitVerifierStrategyResolver;
	private final UsageRepository usageRepository;

	public LimitVerifierImpl(LimitRuleResolver limitRuleResolver,
			LimitVerificationStrategyResolver limitVerifierStrategyResolver,
			UsageRepository usageRepository) {
		this.limitRuleResolver = limitRuleResolver;
		this.usageRepository = usageRepository;
		this.limitVerifierStrategyResolver = limitVerifierStrategyResolver;
	}

	@Override
	public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		LimitsValidationUtil.validateAdditionalUnits(additionalUnits);
		recordOrReduce(feature, userGrouping, additionalUnits, true);
	}

	@Override
	public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
		if (reducedUnits == null || reducedUnits.isEmpty()) {
			throw new IllegalArgumentException("Reduced units cannot be empty");
		}
		if (reducedUnits.values().stream().anyMatch(v -> v == null || v <= 0)) {
			throw new IllegalArgumentException("Reduced units must be positive numbers");
		}
		recordOrReduce(feature, userGrouping, reducedUnits, false);
	}

	@Override
	public Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);

		usageRepository.loadUsageData(context);

		return limitVerificationStrategiesMap.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue().getRemainingUnits(context, entry.getKey())));
	}

	@Override
	public boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);
		usageRepository.loadUsageData(context);

		return limitVerificationStrategiesMap.entrySet().stream()
				.allMatch(entry -> entry.getValue().isWithinLimits(context, entry.getKey(), additionalUnits.get(entry.getKey().getId())));

	}

	private void recordOrReduce(Feature feature, UserGrouping userGrouping, Map<String, Long> units, boolean isRecord) {

		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);

		usageRepository.loadUsageData(context);

		if (isRecord) {
			limitVerificationStrategiesMap
					.forEach((limitRule, verifStrategy) -> verifStrategy.recordFeatureUsage(context, limitRule, units.get(limitRule.getId())));
		}
		else {
			limitVerificationStrategiesMap
					.forEach((limitRule, verifStrategy) -> verifStrategy.reduceFeatureUsage(context, limitRule, units.get(limitRule.getId())));
		}

		usageRepository.updateUsageRecords(context);
	}

	private List<RecordSearchCriteria> gatherSearchCriteria(Map<LimitRule, LimitVerificationStrategy> verificationStrategyMap) {
		return verificationStrategyMap.entrySet().stream()
				.map(entry -> getLimitSearchCriteria(entry.getValue(), entry.getKey()))
				.toList();
	}

	private Map<LimitRule, LimitVerificationStrategy> findVerificationStrategiesByLimit(Feature feature, UserGrouping userGrouping) {
		return feature.getLimitsIds().stream()
				.map(limitId -> limitRuleResolver.resolveLimitRule(feature, limitId, userGrouping))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toMap(Function.identity(), limitVerifierStrategyResolver::resolveLimitVerificationStrategy));
	}

	private RecordSearchCriteria getLimitSearchCriteria(LimitVerificationStrategy strategy, LimitRule limit) {
		Optional<ZonedDateTime> windowStart = strategy.getWindowStart(limit, ZonedDateTime.now());
		Optional<ZonedDateTime> windowEnd = strategy.getWindowEnd(limit, ZonedDateTime.now());
		return new RecordSearchCriteria(limit.getId(), windowStart.orElse(null),
				windowEnd.orElse(null));
	}
}
