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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.terpomo.pmitz.core.limits.LimitRule;

public class Plan implements Serializable {

	private final Product product;

	private String planId;

	private String description;

	private Set<Feature> includedFeatures;

	private Map<String, Feature> includedFeaturesById;

	private List<LimitRule> limitsOverride = Collections.emptyList();

	public Plan(Product product, String planId, List<String> includedFeatures) {
		this.planId = planId;
		this.product = product;
		setIncludedFeatures(includedFeatures.stream().map(id -> new Feature(product, id)).collect(Collectors.toSet()));
	}

	public Product getProduct() {
		return product;
	}

	public String getPlanId() {
		return planId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Feature> getIncludedFeatures() {
		return includedFeatures;
	}

	public Optional<Feature> getIncludedFeature(String featureId) {
		return Optional.ofNullable(includedFeaturesById.get(featureId));
	}

	public void setIncludedFeatures(Set<Feature> includedFeatures) {
		this.includedFeatures = includedFeatures;
		this.includedFeaturesById = includedFeatures.stream()
				.collect(Collectors.toMap(Feature::getFeatureId, Function.identity()));
	}

	public List<LimitRule> getLimitsOverride() {
		return limitsOverride;
	}

	public void setLimitsOverride(List<LimitRule> limitsOverride) {
		this.limitsOverride = limitsOverride;
	}
}
