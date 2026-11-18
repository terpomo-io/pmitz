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

package io.terpomo.pmitz.limits;

import java.util.Optional;

import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;

public interface LimitRuleResolver {

	/**
	 * Resolves the limit rule that applies to the given feature and user grouping.
	 * @param product the product referenced by {@code featureRef}, already loaded by the caller
	 * @param featureRef the feature the limit belongs to
	 * @param limitRuleId the id of the limit rule
	 * @param userGrouping the user grouping the limit applies to
	 * @return the resolved limit rule, or empty if none applies
	 */
	Optional<LimitRule> resolveLimitRule(Product product, FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping);
}
