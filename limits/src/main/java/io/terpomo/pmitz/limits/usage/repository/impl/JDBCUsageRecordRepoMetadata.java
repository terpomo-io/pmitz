package io.terpomo.pmitz.limits.usage.repository.impl;


import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;

import java.time.ZonedDateTime;

public record JDBCUsageRecordRepoMetadata(long usageId, ZonedDateTime updatedAt) implements UsageRecordRepoMetadata {

}
