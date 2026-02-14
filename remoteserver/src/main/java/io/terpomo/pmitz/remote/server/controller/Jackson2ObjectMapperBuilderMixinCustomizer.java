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

package io.terpomo.pmitz.remote.server.controller;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.repository.product.inmemory.CalendarPeriodRateLimitMixIn;
import io.terpomo.pmitz.core.repository.product.inmemory.CountLimitMixIn;
import io.terpomo.pmitz.core.repository.product.inmemory.FeatureMixIn;
import io.terpomo.pmitz.core.repository.product.inmemory.LimitRuleMixIn;
import io.terpomo.pmitz.core.repository.product.inmemory.PlanMixIn;
import io.terpomo.pmitz.core.repository.product.inmemory.ProductMixIn;

@Configuration
public class Jackson2ObjectMapperBuilderMixinCustomizer implements JsonMapperBuilderCustomizer {
	@Override
	public void customize(JsonMapper.Builder builder) {
		builder
				.addMixIn(Product.class, ProductMixIn.class)
				.addMixIn(Feature.class, FeatureMixIn.class)
				.addMixIn(Plan.class, PlanMixIn.class)
				.addMixIn(LimitRule.class, LimitRuleMixIn.class)
				.addMixIn(CalendarPeriodRateLimit.class, CalendarPeriodRateLimitMixIn.class)
				.addMixIn(CountLimit.class, CountLimitMixIn.class);
	}
}

