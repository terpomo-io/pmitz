package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;

import java.time.ZonedDateTime;
import java.util.Optional;

public record UsageRecord(Feature feature, String limitId, Optional<ZonedDateTime> startTime, Optional<ZonedDateTime> endTime, Long units) {
}
