package io.terpomo.pmitz.limits.usage.repository;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageRecord;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LimitTrackingContextTest {

    Product product = new Product("photo-sharing");
    Feature feature = new Feature(product, "upload-photo");

    String limitId = "number-of-photos";

    @Test
    void addUsageRecordsShouldAddRecordsToUpdatedList() {
        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        UsageRecord usageRecord = new UsageRecord(feature, limitId, Optional.empty(), Optional.empty(), 3L);
        context.addUsageRecords(Collections.singletonList(usageRecord));

        var updatedRecords = context.getUpdatedUsageRecords();
        assertEquals(1, updatedRecords.size());
    }
    @Test
    void findUsageRecordsShouldFilterRecords() {
        ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
        ZonedDateTime endTime = ZonedDateTime.now();

        var usageRecords = getCurrentUsageRecords(startTime, endTime);

        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList(), usageRecords, Collections.emptyList());

        var filteredRecords = context.findUsageRecords(limitId, Optional.of(startTime), Optional.of(endTime));

        assertEquals(3, filteredRecords.size());
    }

    @Test
    void findUsageRecordsShouldFilterRecordsWhenParamsEmpty() {
        ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
        ZonedDateTime endTime = ZonedDateTime.now();

        var usageRecords = getCurrentUsageRecords(startTime, endTime);

        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList(), usageRecords, Collections.emptyList());

        var filteredRecords = context.findUsageRecords(limitId, Optional.empty(), Optional.empty());

        assertEquals(4, filteredRecords.size());
    }

    private List<UsageRecord> getCurrentUsageRecords (ZonedDateTime startTime, ZonedDateTime endTime){
        UsageRecord usageRecord1 = new UsageRecord(feature, limitId, Optional.empty(), Optional.empty(), 3L);
        UsageRecord usageRecord2 = new UsageRecord(feature, limitId, Optional.of(startTime), Optional.empty(), 2L);
        UsageRecord usageRecord3 = new UsageRecord(feature, limitId, Optional.empty(), Optional.of(endTime), 2L);
        UsageRecord usageRecord4 = new UsageRecord(feature, limitId, Optional.of(startTime.minusHours(1)), Optional.of(endTime), 2L);

        return Arrays.asList(usageRecord1, usageRecord2, usageRecord3, usageRecord4);
    }
}