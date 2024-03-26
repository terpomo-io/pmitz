package io.terpomo.pmitz.limits.impl.strategy;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleUsageLimitVerificationStrategyTest {
	static String limitId = "numer-of-photos";
	UsageLimitVerificationStrategy<UsageLimit> verificationStrategy = new SimpleUsageLimitVerificationStrategy<>();
	@Captor
	ArgumentCaptor<List<UsageRecord>> updatedRecordArgCaptor;

	static Stream<Arguments> existingRecords() {
		UsageRecord existingRecord = new UsageRecord(limitId, null, null, 5L, null);
		return Stream.of(
				Arguments.of(Collections.emptyList(), 0),
				Arguments.of(Collections.singletonList(existingRecord), 5)
		);
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void recordFeatureUsageShouldSaveAdditionalUsagedWhenCountLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {

		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);
		verificationStrategy.recordFeatureUsage(context, usageLimit, 5);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(previouslyConsumedUnits + 5, updatedUsageRecord.units());
		assertNull(updatedUsageRecord.startTime());
		assertNull(updatedUsageRecord.endTime());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@Test
	void recordFeatureUsageShouldSaveAdditionalUsageWhenRateLimitAndExistingRecord() {

		var windowEnd = ZonedDateTime.now();
		var windowStart = windowEnd.minusDays(30);

		UsageRecord existingRecord = new UsageRecord(new RepoId("id001"), limitId, windowStart, windowEnd, 5L, windowEnd.plusMonths(1));

		LimitTrackingContext context = initContextMock(windowStart, windowEnd, Collections.singletonList(existingRecord));

		RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd, 10L);

		verificationStrategy.recordFeatureUsage(context, usageLimit, 5);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(5 + 5, updatedUsageRecord.units());
		assertEquals(existingRecord.repoMetadata(), updatedUsageRecord.repoMetadata());
		assertEquals(existingRecord.startTime(), updatedUsageRecord.startTime());
		assertEquals(existingRecord.endTime(), updatedUsageRecord.endTime());
		assertEquals(existingRecord.expirationDate(), updatedUsageRecord.expirationDate());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void recordFeatureUsageShouldSaveAdditionalUsageWhenRateLimitAndNoRecord() {

		var windowEnd = ZonedDateTime.now();
		var windowStart = windowEnd.minusDays(30);

		LimitTrackingContext context = initContextMock(windowStart, windowEnd, Collections.emptyList());

		RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd, 15L);

		verificationStrategy.recordFeatureUsage(context, usageLimit, 10);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(10, updatedUsageRecord.units());
		assertEquals(windowStart, updatedUsageRecord.startTime());
		assertEquals(windowEnd, updatedUsageRecord.endTime());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@Test
	void recordFeatureUsageShouldThrowExceptionWhenLimitExceeded() {
		UsageRecord existingRecord = new UsageRecord(new RepoId("id002"), limitId, null, null, 5L, null);

		LimitTrackingContext context = initContextMock(null, null, Collections.singletonList(existingRecord));

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertThrows(LimitExceededException.class, () -> verificationStrategy.recordFeatureUsage(context, usageLimit, 6));
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void reduceFeatureUsageShouldSubtractUsageWhenCountLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		verificationStrategy.reduceFeatureUsage(context, usageLimit, 5);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(previouslyConsumedUnits == 0 ? 0 : previouslyConsumedUnits - 5, updatedUsageRecord.units());
		assertNull(updatedUsageRecord.startTime());
		assertNull(updatedUsageRecord.endTime());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@Test
	void reduceFeatureUsageShouldSubtractUsageWhenRateLimitAndExistingRecord() {

		var windowEnd = ZonedDateTime.now();
		var windowStart = windowEnd.minusDays(30);

		UsageRecord existingRecord = new UsageRecord(new RepoId("id00X"), limitId, windowStart, windowEnd, 5L, windowStart.plusDays(50));

		LimitTrackingContext context = initContextMock(windowStart, windowEnd, Collections.singletonList(existingRecord));

		RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd);

		verificationStrategy.reduceFeatureUsage(context, usageLimit, 4);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(5 - 4, updatedUsageRecord.units());
		assertEquals(existingRecord.repoMetadata(), updatedUsageRecord.repoMetadata());
		assertEquals(existingRecord.startTime(), updatedUsageRecord.startTime());
		assertEquals(existingRecord.endTime(), updatedUsageRecord.endTime());
		assertEquals(existingRecord.expirationDate(), updatedUsageRecord.expirationDate());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@Test
	void reduceFeatureUsageShouldSubtractUsageWhenRateLimitAndNoRecord() {

		var windowEnd = ZonedDateTime.now();
		var windowStart = windowEnd.minusDays(30);

		LimitTrackingContext context = initContextMock(windowStart, windowEnd, Collections.emptyList());

		RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd);

		verificationStrategy.reduceFeatureUsage(context, usageLimit, 5);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertEquals(1, updatedRecordList.size());
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertEquals(0, updatedUsageRecord.units());
		assertEquals(windowStart, updatedUsageRecord.startTime());
		assertEquals(windowEnd, updatedUsageRecord.endTime());
		assertEquals(windowEnd.plusMonths(3), updatedUsageRecord.expirationDate());
		assertEquals(limitId, updatedUsageRecord.limitId());
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void isWithinLimitsShouldReturnTrueIfLimitNotExceeded(List<UsageRecord> existingRecords) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertTrue(verificationStrategy.isWithinLimits(context, usageLimit, 5));
	}

	@Test
	void isWithinLimitsShouldReturnFalseIfLimitExceeded() {
		UsageRecord existingRecord = new UsageRecord(limitId, null, null, 5L, null);
		LimitTrackingContext context = initContextMock(null, null, Collections.singletonList(existingRecord));

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertFalse(verificationStrategy.isWithinLimits(context, usageLimit, 6));
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void getRemainingUnitsShouldReturnUnusedUnits(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertEquals(10 - previouslyConsumedUnits, verificationStrategy.getRemainingUnits(context, usageLimit));
	}

	@Test
	void getWindowStartShouldDelegateToUsageLimit() {
		var windowStart = ZonedDateTime.now().minusDays(30);
		RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
		when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowStart));

		assertEquals(windowStart, verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).orElse(null));
	}

	@Test
	void getWindowEndShouldDelegateToUsageLimit() {
		var windowEnd = ZonedDateTime.now();
		RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
		when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowEnd));

		assertEquals(windowEnd, verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).orElse(null));
	}

	private LimitTrackingContext initContextMock(ZonedDateTime windowStart, ZonedDateTime windowEnd, List<UsageRecord> existingUsageRecord) {
		LimitTrackingContext context = mock(LimitTrackingContext.class);
		when(context.findUsageRecords(limitId, windowStart, windowEnd)).thenReturn(existingUsageRecord);

		return context;
	}

	private RateLimit initRateLimitMock(ZonedDateTime windowStart, ZonedDateTime windowEnd, long limit) {
		RateLimit usageLimit = initRateLimitMock(windowStart, windowEnd);
		when(usageLimit.getValue()).thenReturn(limit);

		return usageLimit;
	}

	private RateLimit initRateLimitMock(ZonedDateTime windowStart, ZonedDateTime windowEnd) {
		RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
		when(usageLimit.getWindowEnd(any())).thenReturn(Optional.of(windowEnd));
		when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowStart));
		when(usageLimit.getId()).thenReturn(limitId);

		return usageLimit;
	}

	private record RepoId(String uniqueId) implements UsageRecordRepoMetadata {

	}
}