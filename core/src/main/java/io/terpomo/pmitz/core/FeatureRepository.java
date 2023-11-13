package io.terpomo.pmitz.core;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository {

    List<Feature> getFeatures (Product product);

    Optional<Feature> getFeature (Product product, String featureId);

    Optional<UsageLimit> getGlobalLimit (Feature feature, String usageLimitId);

    void addFeature (Feature feature);

    void updateFeature (Feature feature);

    void removeFeature (Feature feature);
}
