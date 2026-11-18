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

package io.terpomo.pmitz.core;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CountLimit;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTests {

	@Test
	void getGlobalLimitShouldReturnEmptyWhenFeatureNotFound() {

		Product p1 = new Product("p1");
		p1.setFeatures(List.of(new Feature(p1, "f1", List.of(new CountLimit("l1", 10)))));

		Optional<LimitRule> limit = p1.getGlobalLimit("f2", "l1");

		assertThat(limit).isEmpty();
	}

	@Test
	void getGlobalLimitShouldReturnEmptyWhenFeatureHasNoLimits() {

		Product p1 = new Product("p1");
		p1.setFeatures(List.of(new Feature(p1, "f1")));

		Optional<LimitRule> limit = p1.getGlobalLimit("f1", "l1");

		assertThat(limit).isEmpty();
	}

	@Test
	void getGlobalLimitShouldReturnEmptyWhenLimitNotFound() {

		Product p1 = new Product("p1");
		p1.setFeatures(List.of(new Feature(p1, "f1", List.of(new CountLimit("l1", 10)))));

		Optional<LimitRule> limit = p1.getGlobalLimit("f1", "l2");

		assertThat(limit).isEmpty();
	}

	@Test
	void getGlobalLimitShouldReturnLimitWhenLimitFound() {

		Product p1 = new Product("p1");
		p1.setFeatures(List.of(new Feature(p1, "f1", List.of(new CountLimit("l1", 10)))));

		Optional<LimitRule> limit = p1.getGlobalLimit("f1", "l1");

		assertThat(limit).isPresent();
		assertThat(limit.get().getId()).isEqualTo("l1");
		assertThat(limit.get().getValue()).isEqualTo(10);
	}
}
