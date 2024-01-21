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

import static org.junit.jupiter.api.Assertions.*;

class LimitTrackingContextTest {

    Product product = new Product("photo-sharing");
    Feature feature = new Feature(product, "upload-photo");

    String limitId = "number-of-photos";

    @Test
    void addUsageRecordsShouldAddRecordsToUpdatedList() {
        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());

        UsageRecord usageRecord = new UsageRecord(limitId, null, null, 3L, null);
        context.addUpdatedUsageRecords(Collections.singletonList(usageRecord));

        var updatedRecords = context.getUpdatedUsageRecords();
        assertEquals(1, updatedRecords.size());
    }
    @Test
    void findUsageRecordsShouldFilterRecords() {
        ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
        ZonedDateTime endTime = ZonedDateTime.now();

        var usageRecords = getCurrentUsageRecords(startTime, endTime);

        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());
        context.addCurrentUsageRecords(usageRecords);

        var filteredRecords = context.findUsageRecords(limitId, startTime, endTime);

        assertEquals(3, filteredRecords.size());
    }

    @Test
    void findUsageRecordsShouldFilterRecordsWhenParamsEmpty() {
        ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
        ZonedDateTime endTime = ZonedDateTime.now();

        var usageRecords = getCurrentUsageRecords(startTime, endTime);

        LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());
        context.addCurrentUsageRecords(usageRecords);

        var filteredRecords = context.findUsageRecords(limitId, null, null);

        assertEquals(4, filteredRecords.size());
    }

    private List<UsageRecord> getCurrentUsageRecords (ZonedDateTime startTime, ZonedDateTime endTime){
        UsageRecord usageRecord1 = new UsageRecord(limitId, null, null, 3L, null);
        UsageRecord usageRecord2 = new UsageRecord(limitId, null, null, 2L, null);
        UsageRecord usageRecord3 = new UsageRecord(limitId, null, null, 2L, null);
        UsageRecord usageRecord4 = new UsageRecord(limitId, startTime.minusHours(1), endTime, 2L, null);

        return Arrays.asList(usageRecord1, usageRecord2, usageRecord3, usageRecord4);
    }
}