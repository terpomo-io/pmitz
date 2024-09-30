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

package io.terpomo.pmitz.limits.impl;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitResolver;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategyResolver;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierImplTests {

	@Mock
	UsageLimitResolver usageLimitResolver;

	@Mock
	UsageRepository usageRepo;

	@Mock
	UsageLimitVerificationStrategy limitVerificationStrategy;

	@Mock
	UsageLimitVerificationStrategyResolver limitVerificationStrategyResolver;

	@Captor
	ArgumentCaptor<LimitTrackingContext> contextArgCaptor;

	@Captor
	ArgumentCaptor<UsageLimit> usageLimitArgCaptor;

	Feature feature;

	UserGrouping userGrouping = new IndividualUser("user001");

	UsageLimit usageLimit = new CountLimit("MAX_FILES", 10L);

	ZonedDateTime zonedDateTime;

	UsageLimitVerifierImpl usageLimitVerifier;

	@BeforeEach
	void init() {
		Product product = new Product("FILE_SHARING");
		feature = new Feature(product, "ADD_FILE");
		feature.getLimits().add(usageLimit);

		usageLimitVerifier = new UsageLimitVerifierImpl(usageLimitResolver, limitVerificationStrategyResolver, usageRepo);
	}

	@Test
	void recordUsageShouldCallLimitVerificationStrategyAndRepoUpdate() {
		initMocks();

		try (MockedStatic<ZonedDateTime> mockedLocalDateTime = mockStatic(ZonedDateTime.class)) {
			mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime);

			usageLimitVerifier.recordFeatureUsage(feature, userGrouping, Collections.singletonMap("MAX_FILES", 2L));
		}

		verify(limitVerificationStrategy).recordFeatureUsage(contextArgCaptor.capture(), usageLimitArgCaptor.capture(), eq(2L));

		var capturedUsageLimit = usageLimitArgCaptor.getValue();
		assertThat(capturedUsageLimit).isEqualTo(usageLimit);

		var capturedContext = contextArgCaptor.getValue();

		var searchCriteriaList = capturedContext.getSearchCriteria();
		assertThat(searchCriteriaList).hasSize(1);
		var searchCriteria = searchCriteriaList.get(0);
		assertThat(searchCriteria.limitId()).isEqualTo("MAX_FILES");
		assertThat(searchCriteria.windowStart()).isNull();
		assertThat(searchCriteria.windowEnd()).isNull();

		assertThat(capturedContext.getFeature()).isEqualTo(feature);
		assertThat(capturedContext.getUserGrouping()).isEqualTo(userGrouping);

		verify(usageRepo).updateUsageRecords(capturedContext);
	}

	@Test
	void reduceFeatureUsageShouldCallLimitVerificationStrategyAndRepo() {
		initMocks();

		try (MockedStatic<ZonedDateTime> mockedLocalDateTime = mockStatic(ZonedDateTime.class)) {
			mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime);

			usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, Collections.singletonMap("MAX_FILES", 2L));
		}
		verify(limitVerificationStrategy).reduceFeatureUsage(contextArgCaptor.capture(), usageLimitArgCaptor.capture(), eq(2L));

		var capturedUsageLimit = usageLimitArgCaptor.getValue();
		assertThat(capturedUsageLimit).isEqualTo(usageLimit);

		var capturedContext = contextArgCaptor.getValue();

		var searchCriteriaList = capturedContext.getSearchCriteria();
		assertThat(searchCriteriaList).hasSize(1);
		var searchCriteria = searchCriteriaList.get(0);
		assertThat(searchCriteria.limitId()).isEqualTo("MAX_FILES");
		assertThat(searchCriteria.windowStart()).isNull();
		assertThat(searchCriteria.windowEnd()).isNull();

		assertThat(capturedContext.getFeature()).isEqualTo(feature);
		assertThat(capturedContext.getUserGrouping()).isEqualTo(userGrouping);

		verify(usageRepo).updateUsageRecords(capturedContext);
	}

	@Test
	void getLimitsRemainingUnitsShouldCallStrategyAndRepositoryLoadUsageData() {
		initMocks();
		when(limitVerificationStrategy.getRemainingUnits(any(), eq(usageLimit))).thenReturn(2L);

		try (MockedStatic<ZonedDateTime> mockedLocalDateTime = mockStatic(ZonedDateTime.class)) {
			mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime);

			var remainingUnitsMap = usageLimitVerifier.getLimitsRemainingUnits(feature, userGrouping);
			assertThat(remainingUnitsMap).hasSize(1);
			assertThat(remainingUnitsMap).containsEntry(usageLimit.getId(), 2L);
		}
		verify(usageRepo).loadUsageData(contextArgCaptor.capture());

		var capturedContext = contextArgCaptor.getValue();

		verify(limitVerificationStrategy).getRemainingUnits(capturedContext, usageLimit);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void isWithinLimitsShouldCallStrategyAndRepositoryLoadUsageData(boolean expectedResult) {
		initMocks();
		long requiredAdditionalUnits = 3L;
		when(limitVerificationStrategy.isWithinLimits(any(), eq(usageLimit), eq(requiredAdditionalUnits))).thenReturn(expectedResult);

		try (MockedStatic<ZonedDateTime> mockedLocalDateTime = mockStatic(ZonedDateTime.class)) {
			mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime);

			var isWithinLimits = usageLimitVerifier.isWithinLimits(feature, userGrouping, Collections.singletonMap(usageLimit.getId(), requiredAdditionalUnits));
			assertThat(isWithinLimits).isEqualTo(expectedResult);
		}
		verify(usageRepo).loadUsageData(contextArgCaptor.capture());

		var capturedContext = contextArgCaptor.getValue();

		verify(limitVerificationStrategy).isWithinLimits(capturedContext, usageLimit, requiredAdditionalUnits);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void recordUsageWhenAdditionalUnitsEmptyShouldThrowException(Map<String, Long> additionalUnits) {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> usageLimitVerifier.recordFeatureUsage(feature, userGrouping, additionalUnits));

	}

	@Test
	void recordUsageWhenAdditionalUnitsNegativeShouldThrowException() {

		var additionalUnits = Collections.singletonMap("MAX_FILES", -1L);
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> usageLimitVerifier.recordFeatureUsage(feature, userGrouping, additionalUnits));

	}

	@ParameterizedTest
	@NullAndEmptySource
	void reduceUsageWhenReducedUnitsEmptyShouldThrowException(Map<String, Long> additionalUnits) {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, additionalUnits));

	}

	@Test
	void reduceUsageWhenReducedlUnitsNegativeShouldThrowException() {

		var additionalUnits = Collections.singletonMap("MAX_FILES", -1L);
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, additionalUnits));

	}

	private void initMocks() {
		when(usageLimitResolver.resolveUsageLimit(feature, "MAX_FILES", userGrouping))
				.thenReturn(Optional.of(usageLimit));

		when(limitVerificationStrategyResolver.resolveLimitVerificationStrategy(usageLimit))
				.thenReturn(limitVerificationStrategy);

		String instantExpected = "2022-03-14T09:33:52Z";
		zonedDateTime = ZonedDateTime.parse(instantExpected);

		when(limitVerificationStrategy.getWindowStart(usageLimit, zonedDateTime)).thenReturn(Optional.empty());
	}

}
