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

package io.terpomo.pmitz.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.terpomo.pmitz.core.limits.UsageLimit;

public class Feature implements Serializable {

	private final String featureId;

	private final Product product;

	private final List<UsageLimit> limits = new ArrayList<>();

	public Feature(Product product, String featureId) {

		this.product = product;
		this.featureId = featureId;
	}

	public String getFeatureId() {
		return featureId;
	}

	public Product getProduct() {
		return product;
	}

	public List<UsageLimit> getLimits() {
		return limits;
	}

	public List<String> getLimitsIds() {
		return limits.stream().map(UsageLimit::getId).toList();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Feature feature = (Feature) o;
		return Objects.equals(featureId, feature.featureId) && Objects.equals(product, feature.product);
	}

	@Override
	public int hashCode() {
		return Objects.hash(featureId, product);
	}
}
