package io.terpomo.pmitz.limits.usage.repository;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.List;

public interface UsageRepository {


    LimitTrackingContext loadUsageData(Feature feature, UserGrouping userGrouping, List<RecordSearchCriteria> searchCriteria);

    void updateUsageRecords(LimitTrackingContext limitTrackingContext);

}
