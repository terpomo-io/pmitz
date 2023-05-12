package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.Optional;

public interface GlobalLimitsDefinition {

    void setLimit (Feature feature, UsageLimit limit);

    void removeLimit (Feature feature);

    Optional<UsageLimit> getLimit (Feature feature);
}
