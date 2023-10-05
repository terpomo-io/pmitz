package io.terpomo.pmitz.core;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Feature {

    private String featureId;

    private Product product;

    private List<UsageLimit> limits;

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public List<UsageLimit> getLimits() {
        return limits;
    }

    public void setLimits(List<UsageLimit> limits) {
        this.limits = limits;
    }

    public List<String> getLimitsIds() {
        return limits == null ? Collections.emptyList() :
                limits.stream().map(UsageLimit::getId).collect(Collectors.toList());
    }

}
