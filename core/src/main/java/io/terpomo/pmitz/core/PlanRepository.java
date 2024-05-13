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
