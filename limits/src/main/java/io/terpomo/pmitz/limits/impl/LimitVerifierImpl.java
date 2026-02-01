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

package io.terpomo.pmitz.limits.impl;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
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
	private final ProductRepository productRepository;

	public LimitVerifierImpl(LimitRuleResolver limitRuleResolver,
			LimitVerificationStrategyResolver limitVerifierStrategyResolver,
			UsageRepository usageRepository,
			ProductRepository productRepository) {
		this.limitRuleResolver = limitRuleResolver;
		this.usageRepository = usageRepository;
		this.limitVerifierStrategyResolver = limitVerifierStrategyResolver;
		this.productRepository = productRepository;
	}

	@Override
	public void recordFeatureUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		LimitsValidationUtil.validateAdditionalUnits(additionalUnits);
		recordOrReduce(featureRef, userGrouping, additionalUnits, true);
	}

	@Override
	public void reduceFeatureUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
		if (reducedUnits == null || reducedUnits.isEmpty()) {
			throw new IllegalArgumentException("Reduced units cannot be empty");
		}
		if (reducedUnits.values().stream().anyMatch(v -> v == null || v <= 0)) {
			throw new IllegalArgumentException("Reduced units must be positive numbers");
		}
		recordOrReduce(featureRef, userGrouping, reducedUnits, false);
	}

	@Override
	public Map<String, Long> getLimitsRemainingUnits(FeatureRef featureRef, UserGrouping userGrouping) {
		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(featureRef, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(featureRef, userGrouping, limitSearchCriteriaList);

		usageRepository.loadUsageData(context);

		return limitVerificationStrategiesMap.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue().getRemainingUnits(context, entry.getKey())));
	}

	@Override
	public boolean isWithinLimits(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(featureRef, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(featureRef, userGrouping, limitSearchCriteriaList);
		usageRepository.loadUsageData(context);

		return limitVerificationStrategiesMap.entrySet().stream()
				.allMatch(entry -> entry.getValue().isWithinLimits(context, entry.getKey(), additionalUnits.get(entry.getKey().getId())));

	}

	private Feature resolveFeature(FeatureRef featureRef) {
		return productRepository.getProductById(featureRef.productId())
				.flatMap(product -> productRepository.getFeature(product, featureRef.featureId()))
				.orElseThrow(() -> new IllegalArgumentException(
						"Feature not found: " + featureRef.productId() + "/" + featureRef.featureId()));
	}

	private void recordOrReduce(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> units, boolean isRecord) {
		var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(featureRef, userGrouping);

		var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

		var context = new LimitTrackingContext(featureRef, userGrouping, limitSearchCriteriaList);

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

	private Map<LimitRule, LimitVerificationStrategy> findVerificationStrategiesByLimit(FeatureRef featureRef, UserGrouping userGrouping) {
		Feature feature = resolveFeature(featureRef);
		return feature.getLimitsIds().stream()
				.map(limitId -> limitRuleResolver.resolveLimitRule(featureRef, limitId, userGrouping))
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
