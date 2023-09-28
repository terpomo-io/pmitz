package io.terpomo.pmitz.limits.usage.repository;

import java.time.ZonedDateTime;
import java.util.Optional;

public record RecordSearchCriteria(String limitId, Optional<ZonedDateTime> windowStart, Optional<ZonedDateTime> windowEnd) {
}
