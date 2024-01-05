package io.terpomo.pmitz.limits.impl.strategy;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleUsageLimitVerificationStrategyTest {
    UsageLimitVerificationStrategy verificationStrategy = new SimpleUsageLimitVerificationStrategy<>();

    static Product product = new Product("photo-sharing");
    static Feature feature = new Feature(product, "upload-photo");

    static String limitId = "numer-of-photos";

    @Captor
    ArgumentCaptor<List<UsageRecord>> updatedRecordArgCaptor;

    @ParameterizedTest
    @MethodSource("existingRecords")
    void recordFeatureUsageShouldSaveAdditionalUsagedWhenCountLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {

        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), existingRecords);

        CountLimit usageLimit = new CountLimit(limitId, 10);
        verificationStrategy.recordFeatureUsage(context, usageLimit, 5);

        verify(context).addUsageRecords(updatedRecordArgCaptor.capture());

        var updatedRecordList = updatedRecordArgCaptor.getValue();

        assertEquals(1, updatedRecordList.size());
        UsageRecord updatedUsageRecord = updatedRecordList.get(0);
        assertEquals(previouslyConsumedUnits + 5, updatedUsageRecord.units());
        assertTrue(updatedUsageRecord.startTime().isEmpty());
        assertTrue(updatedUsageRecord.endTime().isEmpty());
        assertEquals(limitId, updatedUsageRecord.limitId());
    }

    @ParameterizedTest
    @MethodSource("existingRecords")
    void recordFeatureUsageShouldSaveAdditionalUsageWhenRateLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {

        var windowEnd = ZonedDateTime.now();
        var windowStart = windowEnd.minusDays(30);

        LimitTrackingContext context = initContextMock(Optional.of(windowStart), Optional.of(windowEnd), existingRecords);

        RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd, 10L);

        verificationStrategy.recordFeatureUsage(context, usageLimit, 5);

        verify(context).addUsageRecords(updatedRecordArgCaptor.capture());

        var updatedRecordList = updatedRecordArgCaptor.getValue();

        assertEquals(1, updatedRecordList.size());
        UsageRecord updatedUsageRecord = updatedRecordList.get(0);
        assertEquals(previouslyConsumedUnits + 5, updatedUsageRecord.units());
        assertEquals(windowStart, updatedUsageRecord.startTime().get());
        assertEquals(windowEnd, updatedUsageRecord.endTime().get());
        assertEquals(limitId, updatedUsageRecord.limitId());
    }

    @Test
    void recordFeatureUsageShouldThrowExceptionWhenLimitExceeded(){
        UsageRecord existingRecord = new UsageRecord(feature, limitId, Optional.empty(), Optional.empty(), 5L);

        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), Collections.singletonList(existingRecord));

        CountLimit usageLimit = new CountLimit(limitId, 10);

        LimitExceededException exception = assertThrows(LimitExceededException.class, ()-> verificationStrategy.recordFeatureUsage(context, usageLimit, 6));
    }

    @ParameterizedTest
    @MethodSource("existingRecords")
    void reduceFeatureUsageShouldSubtractUsageWhenCountLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), existingRecords);

        CountLimit usageLimit = new CountLimit(limitId, 10);

        verificationStrategy.reduceFeatureUsage(context, usageLimit, 5);

        verify(context).addUsageRecords(updatedRecordArgCaptor.capture());

        var updatedRecordList = updatedRecordArgCaptor.getValue();

        assertEquals(1, updatedRecordList.size());
        UsageRecord updatedUsageRecord = updatedRecordList.get(0);
        assertEquals(previouslyConsumedUnits == 0 ? 0 : previouslyConsumedUnits - 5, updatedUsageRecord.units());
        assertTrue(updatedUsageRecord.startTime().isEmpty());
        assertTrue(updatedUsageRecord.endTime().isEmpty());
        assertEquals(limitId, updatedUsageRecord.limitId());
    }

    @ParameterizedTest
    @MethodSource("existingRecords")
    void reduceFeatureUsageShouldSubtractUsageWhenRateLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {

        var windowEnd = ZonedDateTime.now();
        var windowStart = windowEnd.minusDays(30);

        LimitTrackingContext context = initContextMock(Optional.of(windowStart), Optional.of(windowEnd), existingRecords);

        RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd);

        verificationStrategy.reduceFeatureUsage(context, usageLimit, 5);

        verify(context).addUsageRecords(updatedRecordArgCaptor.capture());

        var updatedRecordList = updatedRecordArgCaptor.getValue();

        assertEquals(1, updatedRecordList.size());
        UsageRecord updatedUsageRecord = updatedRecordList.get(0);
        assertEquals(previouslyConsumedUnits == 0 ? 0 : previouslyConsumedUnits - 5, updatedUsageRecord.units());
        assertEquals(windowStart, updatedUsageRecord.startTime().get());
        assertEquals(windowEnd, updatedUsageRecord.endTime().get());
        assertEquals(limitId, updatedUsageRecord.limitId());
    }

    @ParameterizedTest
    @MethodSource("existingRecords")
    void isWithinLimitsShouldReturnTrueIfLimitNotExceeded(List<UsageRecord> existingRecords) {
        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), existingRecords, false);

        CountLimit usageLimit = new CountLimit(limitId, 10);

        assertTrue(verificationStrategy.isWithinLimits(context, usageLimit, 5));
    }

    @Test
    void isWithinLimitsShouldReturnFalseIfLimitExceeded() {
        UsageRecord existingRecord = new UsageRecord(feature, limitId, Optional.empty(), Optional.empty(), 5L);
        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), Collections.singletonList(existingRecord), false);

        CountLimit usageLimit = new CountLimit(limitId, 10);

        assertFalse(verificationStrategy.isWithinLimits(context, usageLimit, 6));
    }

    @ParameterizedTest
    @MethodSource("existingRecords")
    void getRemainingUnitsShouldReturnUnusedUnits(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
        LimitTrackingContext context = initContextMock(Optional.empty(), Optional.empty(), existingRecords, false);

        CountLimit usageLimit = new CountLimit(limitId, 10);

        assertEquals(10 - previouslyConsumedUnits, verificationStrategy.getRemainingUnits(context, usageLimit));
    }

    @Test
    void getWindowStartShouldDelegateToUsageLimit() {
        var windowStart = ZonedDateTime.now().minusDays(30);
        RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
        when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowStart));

        assertEquals(windowStart, verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).get());
    }

    @Test
    void getWindowEndShouldDelegateToUsageLimit() {
        var windowEnd = ZonedDateTime.now();
        RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
        when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowEnd));

        assertEquals(windowEnd, verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).get());
    }

    private LimitTrackingContext initContextMock(Optional<ZonedDateTime> windowStart, Optional<ZonedDateTime> windowEnd, List<UsageRecord> existingUsageRecord) {
        return initContextMock(windowStart, windowEnd, existingUsageRecord, true);
    }

    private LimitTrackingContext initContextMock(Optional<ZonedDateTime> windowStart, Optional<ZonedDateTime> windowEnd, List<UsageRecord> existingUsageRecord, boolean initFeature) {
        LimitTrackingContext context = mock(LimitTrackingContext.class);
        if (initFeature) {
            when(context.getFeature()).thenReturn(feature);
        }
        when(context.findUsageRecords(limitId, windowStart, windowEnd)).thenReturn(existingUsageRecord);

        return context;
    }

    private RateLimit initRateLimitMock (ZonedDateTime windowStart, ZonedDateTime windowEnd, long limit){
        RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd);
        when(usageLimit.getValue()).thenReturn(limit);

        return usageLimit;
    }

    private RateLimit initRateLimitMock (ZonedDateTime windowStart, ZonedDateTime windowEnd){
        RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
        when(usageLimit.getWindowEnd(any())).thenReturn(Optional.of(windowEnd));
        when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowStart));
        when(usageLimit.getId()).thenReturn(limitId);

        return usageLimit;
    }

    static Stream<Arguments> existingRecords() {
        UsageRecord existingRecord = new UsageRecord(feature, limitId, Optional.empty(), Optional.empty(), 5L);
        return Stream.of(
                Arguments.of(Collections.emptyList(), 0),
                Arguments.of(Collections.singletonList(existingRecord), 5)
        );
    }
}