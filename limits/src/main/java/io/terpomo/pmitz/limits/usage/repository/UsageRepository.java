package io.terpomo.pmitz.limits.usage.repository;

public interface UsageRepository {

    void loadUsageData(LimitTrackingContext limitTrackingContext);

    void updateUsageRecords(LimitTrackingContext limitTrackingContext);

}
