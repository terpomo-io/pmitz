package io.terpomo.pmitz.limits.usage.repository.impl;


import java.time.ZonedDateTime;

import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;

public record JDBCUsageRecordRepoMetadata(long usageId, ZonedDateTime updatedAt) implements UsageRecordRepoMetadata {

}
