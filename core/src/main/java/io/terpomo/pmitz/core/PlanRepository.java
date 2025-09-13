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

import java.util.List;
import java.util.Optional;

public interface PlanRepository {

	List<Plan> getPlans(Product product);

	Optional<Plan> getPlan(Product product, String planId);

	void addPlan(Plan plan);

	void updatePlan(Plan plan);

	void removePlan(Plan plan);

	void disablePlan(Plan plan);

	boolean isIncluded(Plan plan, Feature feature);

	void addFeature(Plan plan, Feature feature);

	void removeFeature(Plan plan, Feature feature);

	Optional<Feature> getFeature(Plan plan, String featureId);
}
