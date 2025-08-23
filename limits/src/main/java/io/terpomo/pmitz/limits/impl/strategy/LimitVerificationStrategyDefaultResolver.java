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

package io.terpomo.pmitz.limits.impl.strategy;

import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.limits.LimitVerificationStrategy;
import io.terpomo.pmitz.limits.LimitVerificationStrategyResolver;

public class LimitVerificationStrategyDefaultResolver implements LimitVerificationStrategyResolver {

	private final LimitVerificationStrategy defaultVerificationStrategy;

	public LimitVerificationStrategyDefaultResolver() {
		defaultVerificationStrategy = new SimpleLimitVerificationStrategy();
	}

	public LimitVerificationStrategyDefaultResolver(LimitVerificationStrategy defaultVerificationStrategy) {
		this.defaultVerificationStrategy = defaultVerificationStrategy;
	}

	@Override
	public LimitVerificationStrategy resolveLimitVerificationStrategy(LimitRule limitRule) {
		return defaultVerificationStrategy;
	}
}
