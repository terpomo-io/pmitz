package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;

public interface UsageCounter {

    void countUsage (Feature feature, String subscriptionId, int units);

    void discountUsage (Feature feature, String subscriptionId, int units);
}
