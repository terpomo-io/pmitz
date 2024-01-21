package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;

import java.time.ZonedDateTime;

public record UsageRecord(UsageRecordRepoMetadata repoMetadata, String limitId, ZonedDateTime startTime, ZonedDateTime endTime, Long units, ZonedDateTime expirationDate) {

    public UsageRecord(String limitId, ZonedDateTime startTime, ZonedDateTime endTime, Long units, ZonedDateTime expirationDate){
        this (null, limitId, startTime, endTime, units, expirationDate);
    }

    public static UsageRecord updage (UsageRecord usageRecord, Long units, ZonedDateTime expirationDate){
        return new UsageRecord(usageRecord.repoMetadata(), usageRecord.limitId(), usageRecord.startTime, usageRecord.endTime, units, expirationDate);
    }

}
