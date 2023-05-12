package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.Optional;

public interface PlansLimitsDefinition {

    void setLimit (Feature feature, Plan plan, UsageLimit usageLimit);

    void removeLimit (Feature feature, Plan plan);

    Optional<UsageLimit> getLimit (Feature feature, Plan plan);
}
