package io.terpomo.pmitz.core;

import java.util.List;

public class Feature {

    private String featureId;

    private Product product;

    private List<String> limitsIds;

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public List<String> getLimitsIds() {
        return limitsIds;
    }

    public void setLimitsIds(List<String> limitsIds) {
        this.limitsIds = limitsIds;
    }
}
