package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface UsageCounter {

    void countUsage (Feature feature, String limitId, UserGrouping userGrouping, int units);

    void discountUsage (Feature feature, String limitId, UserGrouping userGrouping, int units);

    Optional<Integer> getUsageCount (Feature feature, String limitId, UserGrouping userGrouping, Optional<ZonedDateTime> startTime, ZonedDateTime endTime);
}
