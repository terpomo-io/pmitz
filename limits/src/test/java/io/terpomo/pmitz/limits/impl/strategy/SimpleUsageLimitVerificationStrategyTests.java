/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.limits.impl.strategy;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleUsageLimitVerificationStrategyTests {
	static String limitId = "numer-of-photos";
	UsageLimitVerificationStrategy verificationStrategy = new SimpleUsageLimitVerificationStrategy();
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

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isEqualTo(previouslyConsumedUnits + 5);
		assertThat(updatedUsageRecord.startTime()).isNull();
		assertThat(updatedUsageRecord.endTime()).isNull();
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
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

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isEqualTo(5 + 5);
		assertThat(updatedUsageRecord.repoMetadata()).isEqualTo(existingRecord.repoMetadata());
		assertThat(updatedUsageRecord.startTime()).isEqualTo(existingRecord.startTime());
		assertThat(updatedUsageRecord.endTime()).isEqualTo(existingRecord.endTime());
		assertThat(updatedUsageRecord.expirationDate()).isEqualTo(existingRecord.expirationDate());
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
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

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isEqualTo(10);
		assertThat(updatedUsageRecord.startTime()).isEqualTo(windowStart);
		assertThat(updatedUsageRecord.endTime()).isEqualTo(windowEnd);
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
	}

	@Test
	void recordFeatureUsageShouldThrowExceptionWhenLimitExceeded() {
		UsageRecord existingRecord = new UsageRecord(new RepoId("id002"), limitId, null, null, 5L, null);

		LimitTrackingContext context = initContextMock(null, null, Collections.singletonList(existingRecord));

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertThatExceptionOfType(LimitExceededException.class).isThrownBy(() -> verificationStrategy.recordFeatureUsage(context, usageLimit, 6));
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void reduceFeatureUsageShouldSubtractUsageWhenCountLimit(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		verificationStrategy.reduceFeatureUsage(context, usageLimit, 5);

		verify(context).addUpdatedUsageRecords(updatedRecordArgCaptor.capture());

		var updatedRecordList = updatedRecordArgCaptor.getValue();

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isEqualTo((previouslyConsumedUnits == 0) ? 0 : previouslyConsumedUnits - 5);
		assertThat(updatedUsageRecord.startTime()).isNull();
		assertThat(updatedUsageRecord.endTime()).isNull();
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
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

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isEqualTo(5 - 4);
		assertThat(updatedUsageRecord.repoMetadata()).isEqualTo(existingRecord.repoMetadata());
		assertThat(updatedUsageRecord.startTime()).isEqualTo(existingRecord.startTime());
		assertThat(updatedUsageRecord.endTime()).isEqualTo(existingRecord.endTime());
		assertThat(updatedUsageRecord.expirationDate()).isEqualTo(existingRecord.expirationDate());
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
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

		assertThat(updatedRecordList).hasSize(1);
		UsageRecord updatedUsageRecord = updatedRecordList.get(0);
		assertThat(updatedUsageRecord.units()).isZero();
		assertThat(updatedUsageRecord.startTime()).isEqualTo(windowStart);
		assertThat(updatedUsageRecord.endTime()).isEqualTo(windowEnd);
		assertThat(updatedUsageRecord.expirationDate()).isEqualTo(windowEnd.plusMonths(3));
		assertThat(updatedUsageRecord.limitId()).isEqualTo(limitId);
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void isWithinLimitsShouldReturnTrueIfLimitNotExceeded(List<UsageRecord> existingRecords) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertThat(verificationStrategy.isWithinLimits(context, usageLimit, 5)).isTrue();
	}

	@Test
	void isWithinLimitsShouldReturnFalseIfLimitExceeded() {
		UsageRecord existingRecord = new UsageRecord(limitId, null, null, 5L, null);
		LimitTrackingContext context = initContextMock(null, null, Collections.singletonList(existingRecord));

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertThat(verificationStrategy.isWithinLimits(context, usageLimit, 6)).isFalse();
	}

	@ParameterizedTest
	@MethodSource("existingRecords")
	void getRemainingUnitsShouldReturnUnusedUnits(List<UsageRecord> existingRecords, int previouslyConsumedUnits) {
		LimitTrackingContext context = initContextMock(null, null, existingRecords);

		CountLimit usageLimit = new CountLimit(limitId, 10);

		assertThat(verificationStrategy.getRemainingUnits(context, usageLimit)).isEqualTo(10 - previouslyConsumedUnits);
	}

	@Test
	void getWindowStartShouldDelegateToUsageLimit() {
		var windowStart = ZonedDateTime.now().minusDays(30);
		RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
		when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowStart));

		assertThat(verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).orElse(null)).isEqualTo(windowStart);
	}

	@Test
	void getWindowEndShouldDelegateToUsageLimit() {
		var windowEnd = ZonedDateTime.now();
		RateLimit usageLimit = mock(CalendarPeriodRateLimit.class);
		when(usageLimit.getWindowStart(any())).thenReturn(Optional.of(windowEnd));

		assertThat(verificationStrategy.getWindowStart(usageLimit, ZonedDateTime.now()).orElse(null)).isEqualTo(windowEnd);
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
