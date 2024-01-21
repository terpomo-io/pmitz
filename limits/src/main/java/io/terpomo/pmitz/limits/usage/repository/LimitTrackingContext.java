package io.terpomo.pmitz.limits.usage.repository;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LimitTrackingContext {

    private final Feature feature;

    private final UserGrouping userGrouping;
    private final List<RecordSearchCriteria> searchCriteria;

    private final List<UsageRecord> currentUsageRecords;

    private final List<UsageRecord> updatedUsageRecords;

    LimitTrackingContext(Feature feature, UserGrouping userGrouping, List<RecordSearchCriteria> searchCriteria, List<UsageRecord> currentUsageRecords, List<UsageRecord> updatedUsageRecords) {
        this.feature = feature;
        this.userGrouping = userGrouping;
        this.searchCriteria = searchCriteria;
        this.currentUsageRecords = currentUsageRecords;
        this.updatedUsageRecords = new ArrayList<>();
        this.updatedUsageRecords.addAll(updatedUsageRecords);
    }

    public Feature getFeature() {
        return feature;
    }

    public UserGrouping getUserGrouping() {
        return userGrouping;
    }

    public List<RecordSearchCriteria> getSearchCriteria() {
        return searchCriteria;
    }

    public List<UsageRecord> getCurrentUsageRecords() {
        return currentUsageRecords;
    }

    public List<UsageRecord> getUpdatedUsageRecords() {
        return updatedUsageRecords;
    }

    public void addUsageRecords (List<UsageRecord> additionalUsageRecords){
        updatedUsageRecords.addAll(additionalUsageRecords);
    }

    public List<UsageRecord> findUsageRecords (String limitId, ZonedDateTime startTime, ZonedDateTime endTime){

        Predicate<UsageRecord> filterCondition = usageRecord -> limitId.equals(usageRecord.limitId())
                && (startTime == null || usageRecord.startTime() == null || usageRecord.startTime().isAfter(startTime) || usageRecord.startTime().isEqual(startTime))
                && (endTime == null || usageRecord.endTime() == null || usageRecord.endTime().isBefore(endTime) || usageRecord.endTime().isEqual(endTime));

        return getCurrentUsageRecords().stream().filter(filterCondition).collect(Collectors.toList());
    }
}
