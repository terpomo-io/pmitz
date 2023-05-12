package io.terpomo.pmitz.core;

import java.util.List;
import java.util.Optional;

public interface FeatureStore {

    List<Feature> getFeatures (Product product);

    Optional<Feature> getFeature (Product product, String featureId);

    void addFeature (Feature feature);

    void updateFeature (Feature feature);

    void removeFeature (Feature feature);
}
