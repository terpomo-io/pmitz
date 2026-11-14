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

import java.util.Collections;
import java.util.Optional;

import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.limits.LimitRuleResolver;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

public class LimitRuleResolverImpl implements LimitRuleResolver {

	private final UserLimitRepository userLimitRepository;

	public LimitRuleResolverImpl() {
		this(new NoOpUserLimitRepository());
	}

	public LimitRuleResolverImpl(UserLimitRepository userLimitRepository) {
		this.userLimitRepository = userLimitRepository;
	}

	@Override
	public Optional<LimitRule> resolveLimitRule(Product product, FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping) {
		var optUserLimitRule = userLimitRepository.findLimitRule(featureRef, limitRuleId, userGrouping);
		if (optUserLimitRule.isPresent()) {
			return optUserLimitRule;
		}

		var optPlanId = userGrouping.getPlan(product.getProductId());

		Optional<LimitRule> planLimitRule = optPlanId.flatMap(product::getPlan)
					.map(Plan::getLimitsOverride)
					.orElseGet(Collections::emptyList)
					.stream().filter(rule -> limitRuleId.equals(rule.getId()))
					.findFirst();

		return planLimitRule.or(() -> product.getGlobalLimit(featureRef.featureId(), limitRuleId));
	}

	public static class NoOpUserLimitRepository implements UserLimitRepository {

		@Override
		public Optional<LimitRule> findLimitRule(FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping) {
			return Optional.empty();
		}

		@Override
		public void updateLimitRule(FeatureRef featureRef, LimitRule limitRule, UserGrouping userGrouping) {
			// No action
		}

		@Override
		public void deleteLimitRule(FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping) {
			// No action
		}
	}
}
