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

package io.terpomo.pmitz.all.usage.tracker.impl;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.LimitVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureUsageTrackerImplTests {

	@Mock
	LimitVerifier limitVerifier;
	@Mock
	SubscriptionVerifier subscriptionVerifier;

	Feature feature;

	UserGrouping userGrouping = new IndividualUser("user001");

	FeatureUsageTrackerImpl featureUsageTracker;

	@BeforeEach
	void init() {
		feature = new Feature(null, "FILE_UPLOAD");

		featureUsageTracker = new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
	}

	@Test
	void givenFeatureNotAllowedWhenRecordFeatureUsageThenThrowFeatureNotAllowedException() {
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

		assertThatExceptionOfType(FeatureNotAllowedException.class).isThrownBy(() -> featureUsageTracker.recordFeatureUsage(feature, userGrouping, Collections.singletonMap("FILE_SIZE", 1000L)));

	}

	@Test
	void givenFeatureAllowedWhenRecordFeatureUsageThenCallLimitVerifier() {
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);

		var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
		featureUsageTracker.recordFeatureUsage(feature, userGrouping, additionalUnits);

		verify(limitVerifier).recordFeatureUsage(feature, userGrouping, additionalUnits);
	}

	@Test
	void givenFeatureWhenReduceFeatureUsageThenCallLimitVerifier() {
		var reducedUnits = Collections.singletonMap("UPLOADED_FILES", 1L);
		featureUsageTracker.reduceFeatureUsage(feature, userGrouping, reducedUnits);

		verify(limitVerifier).reduceFeatureUsage(feature, userGrouping, reducedUnits);
	}

	@Test
	void givenFeatureNotAllowedWhenVerifyLimitsThenFeatureStatusNotAllowed() {
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

		var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
		var featureInfo = featureUsageTracker.verifyLimits(feature, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.NOT_ALLOWED);
		assertThat(featureInfo.remainingUsageUnits()).isEmpty();
	}

	@Test
	void givenAnyLimitExceededWhenVerifyLimitsThenFeatureStatusNotAllowed() {
		String limitId = "FILE_UPLOAD";
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);
		when(limitVerifier.getLimitsRemainingUnits(feature, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

		var additionalUnits = Collections.singletonMap(limitId, 2L);
		var featureInfo = featureUsageTracker.verifyLimits(feature, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.LIMIT_EXCEEDED);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, -1L);
	}

	@Test
	void givenFeatureNotAllowedWhenGetUsageInfoThenReturnUsageInfo() {
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

		FeatureUsageInfo featureUsageInfo = featureUsageTracker.getUsageInfo(feature, userGrouping);
		assertThat(featureUsageInfo.featureStatus()).isEqualTo(FeatureStatus.NOT_ALLOWED);
		assertThat(featureUsageInfo.remainingUsageUnits()).isEmpty();
	}

	@Test
	void givenFeatureAllowedWhenGetUsageInfoThenReturnRemainingUnits() {
		String limitId = "ADD_USER";
		when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);
		when(limitVerifier.getLimitsRemainingUnits(feature, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

		var featureInfo = featureUsageTracker.getUsageInfo(feature, userGrouping);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.AVAILABLE);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, 1L);
	}

}
