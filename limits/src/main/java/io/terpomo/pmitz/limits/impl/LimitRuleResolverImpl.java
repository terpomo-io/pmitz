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

package io.terpomo.pmitz.limits.impl;

import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.LimitRuleResolver;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

public class LimitRuleResolverImpl implements LimitRuleResolver {

	private ProductRepository productRepository;
	private UserLimitRepository userLimitRepository;

	public LimitRuleResolverImpl(ProductRepository productRepository) {
		this(productRepository, new NoOpUserLimitRepository());
	}

	public LimitRuleResolverImpl(ProductRepository productRepository, UserLimitRepository userLimitRepository) {
		this.productRepository = productRepository;
		this.userLimitRepository = userLimitRepository;
	}

	@Override
	public Optional<LimitRule> resolveLimitRule(Feature feature, String limitRuleId, UserGrouping userGrouping) {
		//TODO verify interfaces' specs
		// 1- find limit specific to userGrouping
		// 2- find limit for plan --> 2.1 find plan, 2.2 find limit for plan
		// 3- find global limit

		return userLimitRepository.findLimitRule(feature, limitRuleId, userGrouping)
				.or(() -> productRepository.getGlobalLimit(feature, limitRuleId));

	}

	public static class NoOpUserLimitRepository implements UserLimitRepository {

		@Override
		public Optional<LimitRule> findLimitRule(Feature feature, String limitRuleId, UserGrouping userGrouping) {
			return Optional.empty();
		}

		@Override
		public void updateLimitRule(Feature feature, LimitRule limitRule, UserGrouping userGrouping) {
			// No action
		}

		@Override
		public void deleteLimitRule(Feature feature, String limitRuleId, UserGrouping userGrouping) {
			// No action
		}
	}
}
