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

package io.terpomo.pmitz.core.repository.product.inmemory;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;

@ExcludeFromJacocoGeneratedReport
public class ProductMixIn {

	@JsonProperty("productId")
	String productId;

	@JsonManagedReference
	List<Plan> plans;

	@JsonManagedReference
	List<Feature> features;

	@JsonCreator
	public ProductMixIn(
			@JsonProperty("productId") String productId) {
	}

}
