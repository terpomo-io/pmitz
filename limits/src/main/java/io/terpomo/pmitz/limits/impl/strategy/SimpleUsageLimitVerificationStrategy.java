package io.terpomo.pmitz.limits.impl.strategy;

import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

public class SimpleUsageLimitVerificationStrategy<T extends UsageLimit> implements UsageLimitVerificationStrategy<T> {

    @Override
    public void recordFeatureUsage(LimitTrackingContext context, T usageLimit, long additionalUnits) {
        var now = ZonedDateTime.now();
        long alreadyUsedUnits = findCurrentUsage (context, usageLimit, now);
        long newUnits = alreadyUsedUnits + additionalUnits;

        if (newUnits > usageLimit.getValue()){
            throw new LimitExceededException("Limit will be exceeded if additional units are used. " + alreadyUsedUnits + " are currently used, out of " + usageLimit.getValue() + ".",
                    context.getFeature(), context.getUserGrouping());
        }

        var updatedRecord = new UsageRecord(usageLimit.getId(), getWindowStart(usageLimit, now), getWindowEnd(usageLimit, now), newUnits);

        context.addUsageRecords(Collections.singletonList(updatedRecord));
    }

    @Override
    public void reduceFeatureUsage(LimitTrackingContext context, T usageLimit, long reducedUnits) {
        var now = ZonedDateTime.now();
        long alreadyUsedUnits = findCurrentUsage(context, usageLimit, now);

        var newUnits = alreadyUsedUnits > reducedUnits ? alreadyUsedUnits - reducedUnits : 0;

        var updatedRecord = new UsageRecord(usageLimit.getId(), getWindowStart(usageLimit, now), getWindowEnd(usageLimit, now), newUnits);
        context.addUsageRecords(Collections.singletonList(updatedRecord));
    }

    @Override
    public boolean isWithinLimits(LimitTrackingContext context, T usageLimit, long additionalUnits) {
        return getRemainingUnits(context, usageLimit) >= additionalUnits;
    }

    @Override
    public long getRemainingUnits(LimitTrackingContext context, T usageLimit) {
        var now = ZonedDateTime.now();
        long currentUsage = findCurrentUsage(context, usageLimit, now);
        return usageLimit.getValue() - currentUsage;
    }

    @Override
    public Optional<ZonedDateTime> getWindowStart(T usageLimit, ZonedDateTime referenceDate) {
        return usageLimit.getWindowStart(referenceDate);
    }

    @Override
    public Optional<ZonedDateTime> getWindowEnd(T usageLimit, ZonedDateTime referenceDate) {
        return usageLimit.getWindowEnd(referenceDate);
    }

    private long findCurrentUsage(LimitTrackingContext context, T usageLimit, ZonedDateTime referenceDate) {
        var usageRecordList = context.findUsageRecords(usageLimit.getId(), getWindowStart(usageLimit, referenceDate), getWindowEnd(usageLimit, referenceDate));

        if (usageRecordList.size() > 1){
            throw new IllegalStateException("Inconsistent data found in usage repository. Should find 1 record atmost for this type of limit");
        }

        return (usageRecordList.isEmpty() ? 0 : usageRecordList.get(0).units());
    }
}
