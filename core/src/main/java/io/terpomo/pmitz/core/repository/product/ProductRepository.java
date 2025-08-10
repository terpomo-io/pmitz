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

package io.terpomo.pmitz.core.repository.product;

import java.util.List;
import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.LimitRule;

public interface ProductRepository {

	List<String> getProductIds();

	Optional<Product> getProductById(String productId);

	void addProduct(Product product);

	void removeProduct(Product product);

	List<Feature> getFeatures(Product product);

	Optional<Feature> getFeature(Product product, String featureId);

	Optional<LimitRule> getGlobalLimit(Feature feature, String limitRuleId);

	void addFeature(Feature feature);

	void updateFeature(Feature feature);

	void removeFeature(Feature feature);
}
