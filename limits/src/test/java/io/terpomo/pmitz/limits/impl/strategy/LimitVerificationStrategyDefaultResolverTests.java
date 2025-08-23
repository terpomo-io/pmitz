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

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.limits.LimitVerificationStrategy;
import io.terpomo.pmitz.limits.LimitVerificationStrategyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class LimitVerificationStrategyDefaultResolverTests {

	LimitVerificationStrategyResolver strategyResolver = new LimitVerificationStrategyDefaultResolver();
	private String limitId = "max-number-of-photos";

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenCountLimit() {
		CountLimit countLimit = new CountLimit(limitId, 10);
		LimitVerificationStrategy countLimitVerifStrategy = strategyResolver.resolveLimitVerificationStrategy(countLimit);
		assertThat(countLimitVerifStrategy).isNotNull();
	}

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenCalendarPeriodRateLimit() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);
		LimitVerificationStrategy verifStrategy = strategyResolver.resolveLimitVerificationStrategy(rateLimit);
		assertThat(verifStrategy).isNotNull();
	}

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenSlidingWindowRateLimit() {
		SlidingWindowRateLimit rateLimit = new SlidingWindowRateLimit(limitId, 10, ChronoUnit.MONTHS, 1);
		LimitVerificationStrategy verifStrategy = strategyResolver.resolveLimitVerificationStrategy(rateLimit);
		assertThat(verifStrategy).isNotNull();
	}

}
