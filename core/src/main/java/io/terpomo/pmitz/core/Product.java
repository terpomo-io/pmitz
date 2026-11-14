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

package io.terpomo.pmitz.core;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.terpomo.pmitz.core.limits.LimitRule;

public class Product implements Serializable {

	private final String productId;

	private List<Feature> features = List.of();

	private Map<String, Feature> featuresById = Map.of();

	private List<Plan> plans = List.of();

	public Product(String productId) {

		this.productId = productId;
	}

	public String getProductId() {
		return productId;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public Optional<Feature> getFeature(String featureId) {
		return Optional.ofNullable(featuresById.get(featureId));
	}

	public List<Plan> getPlans() {
		return plans;
	}

	public Optional<Plan> getPlan(String planId) {
		return plans.stream().filter(plan -> plan.getPlanId().equals(planId)).findFirst();
	}

	public Optional<LimitRule> getGlobalLimit(String featureId, String limitRuleId) {
		return getFeature(featureId)
				.flatMap(feature -> feature.getLimits().stream()
						.filter(limit -> limit.getId().equals(limitRuleId))
						.findFirst());
	}

	public void setFeatures(List<Feature> features) {
		this.features = List.copyOf(features);
		featuresById = this.features.stream().collect(Collectors.toUnmodifiableMap(Feature::getFeatureId, Function.identity()));
	}

	public void setPlans(List<Plan> plans) {
		this.plans = List.copyOf(plans);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Product product = (Product) o;
		return Objects.equals(productId, product.productId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(productId);
	}
}
