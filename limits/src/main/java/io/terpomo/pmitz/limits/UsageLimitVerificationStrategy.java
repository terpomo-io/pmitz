package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface UsageLimitVerificationStrategy<T extends UsageLimit>{

    void recordFeatureUsage(LimitTrackingContext context, T usageLimit, long additionalUnits);

    void reduceFeatureUsage(LimitTrackingContext context, T usageLimit, long reducedUnits);

    boolean isWithinLimits (LimitTrackingContext context, T usageLimit, long additionalUnits);

    long getRemainingUnits (LimitTrackingContext context, T usageLimit);

    Optional<ZonedDateTime> getWindowStart (T usageLimit, ZonedDateTime referenceDate);

    Optional<ZonedDateTime> getWindowEnd (T usageLimit, ZonedDateTime referenceDate);
}
