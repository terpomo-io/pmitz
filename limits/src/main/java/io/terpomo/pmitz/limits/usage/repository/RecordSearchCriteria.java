package io.terpomo.pmitz.limits.usage.repository;

import java.time.ZonedDateTime;

public record RecordSearchCriteria(String limitId, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
}
