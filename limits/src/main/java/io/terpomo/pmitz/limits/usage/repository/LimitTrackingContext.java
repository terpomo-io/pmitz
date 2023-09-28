package io.terpomo.pmitz.limits.usage.repository;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;

import java.util.List;
import java.util.Optional;

public class LimitTrackingContext {

    private Feature feature;

    private UserGrouping userGrouping;
    private List<RecordSearchCriteria> searchCriteria;

    private List<UsageRecord> currentUsageRecords;

    private List<UsageRecord> updatedUsageRecords;

    LimitTrackingContext(Feature feature, UserGrouping userGrouping, List<RecordSearchCriteria> searchCriteria, List<UsageRecord> currentUsageRecords, List<UsageRecord> updatedUsageRecords) {
        this.feature = feature;
        this.userGrouping = userGrouping;
        this.searchCriteria = searchCriteria;
        this.currentUsageRecords = currentUsageRecords;
        this.updatedUsageRecords = updatedUsageRecords;
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
}
