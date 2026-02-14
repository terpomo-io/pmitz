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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Product implements Serializable {

	private String productId;

	private List<Feature> features = new ArrayList<>();

	private Map<String, Feature> featuresById = new HashMap<>();

	private List<Plan> plans = new ArrayList<>();

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

	public void setFeatures(List<Feature> features) {
		this.features = features;
		featuresById = features.stream().collect(Collectors.toMap(Feature::getFeatureId, Function.identity()));
	}

	public void setPlans(List<Plan> plans) {
		this.plans = plans;
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
