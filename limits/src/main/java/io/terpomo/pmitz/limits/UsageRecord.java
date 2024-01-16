package io.terpomo.pmitz.limits;

import java.time.ZonedDateTime;
import java.util.Optional;

public record UsageRecord(String limitId, Optional<ZonedDateTime> startTime, Optional<ZonedDateTime> endTime, Long units) {


}
