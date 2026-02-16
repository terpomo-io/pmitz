/*
 * Copyright 2023-2026 the original author or authors.
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

import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;
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

	FeatureRef featureRef;

	UserGrouping userGrouping = new IndividualUser("user001");

	FeatureUsageTrackerImpl featureUsageTracker;

	@BeforeEach
	void init() {
		featureRef = new FeatureRef("test-product", "FILE_UPLOAD");

		featureUsageTracker = new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
	}

	@Test
	void givenFeatureNotAllowedWhenRecordFeatureUsageThenThrowFeatureNotAllowedException() {
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED));

		assertThatExceptionOfType(FeatureNotAllowedException.class).isThrownBy(() -> featureUsageTracker.recordFeatureUsage(featureRef, userGrouping, Collections.singletonMap("FILE_SIZE", 1000L)));

	}

	@Test
	void givenFeatureAllowedWhenRecordFeatureUsageThenCallLimitVerifier() {
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationOk());

		var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
		featureUsageTracker.recordFeatureUsage(featureRef, userGrouping, additionalUnits);

		verify(limitVerifier).recordFeatureUsage(featureRef, userGrouping, additionalUnits);
	}

	@Test
	void givenFeatureWhenReduceFeatureUsageThenCallLimitVerifier() {
		var reducedUnits = Collections.singletonMap("UPLOADED_FILES", 1L);
		featureUsageTracker.reduceFeatureUsage(featureRef, userGrouping, reducedUnits);

		verify(limitVerifier).reduceFeatureUsage(featureRef, userGrouping, reducedUnits);
	}

	@Test
	void givenFeatureNotAllowedWhenVerifyLimitsThenFeatureStatusNotAllowed() {
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED));

		var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
		var featureInfo = featureUsageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.NOT_ALLOWED);
		assertThat(featureInfo.remainingUsageUnits()).isEmpty();
	}

	@Test
	void givenAnyLimitExceededWhenVerifyLimitsThenFeatureStatusNotAllowed() {
		String limitId = "FILE_UPLOAD";
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationOk());
		when(limitVerifier.getLimitsRemainingUnits(featureRef, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

		var additionalUnits = Collections.singletonMap(limitId, 2L);
		var featureInfo = featureUsageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.LIMIT_EXCEEDED);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, -1L);
	}

	@Test
	void givenFeatureNotAllowedWhenGetUsageInfoThenReturnUsageInfo() {
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION));

		FeatureUsageInfo featureUsageInfo = featureUsageTracker.getUsageInfo(featureRef, userGrouping);
		assertThat(featureUsageInfo.featureStatus()).isEqualTo(FeatureStatus.NOT_ALLOWED);
		assertThat(featureUsageInfo.remainingUsageUnits()).isEmpty();
	}

	@Test
	void givenFetchedSubscriptionWhenVerifyLimitsThenUseFetchedSubscriptionForLimits() {
		String limitId = "FILE_UPLOAD";
		var fetchedSubscription = new Subscription("sub001");
		var verifDetail = SubscriptionVerifDetail.verificationOk().withFetchedSubscription(fetchedSubscription);
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(verifDetail);
		when(limitVerifier.getLimitsRemainingUnits(featureRef, fetchedSubscription)).thenReturn(Collections.singletonMap(limitId, 5L));

		var additionalUnits = Collections.singletonMap(limitId, 2L);
		var featureInfo = featureUsageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.AVAILABLE);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, 3L);
		verify(limitVerifier).getLimitsRemainingUnits(featureRef, fetchedSubscription);
		verify(limitVerifier, never()).getLimitsRemainingUnits(featureRef, userGrouping);
	}

	@Test
	void givenFetchedSubscriptionWithExceededLimitWhenVerifyLimitsThenReturnLimitExceeded() {
		String limitId = "FILE_UPLOAD";
		var fetchedSubscription = new Subscription("sub001");
		var verifDetail = SubscriptionVerifDetail.verificationOk().withFetchedSubscription(fetchedSubscription);
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(verifDetail);
		when(limitVerifier.getLimitsRemainingUnits(featureRef, fetchedSubscription)).thenReturn(Collections.singletonMap(limitId, 1L));

		var additionalUnits = Collections.singletonMap(limitId, 5L);
		var featureInfo = featureUsageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.LIMIT_EXCEEDED);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, -4L);
	}

	@Test
	void givenNoFetchedSubscriptionWhenVerifyLimitsThenUseOriginalUserGrouping() {
		String limitId = "FILE_UPLOAD";
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationOk());
		when(limitVerifier.getLimitsRemainingUnits(featureRef, userGrouping)).thenReturn(Collections.singletonMap(limitId, 5L));

		var additionalUnits = Collections.singletonMap(limitId, 2L);
		var featureInfo = featureUsageTracker.verifyLimits(featureRef, userGrouping, additionalUnits);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.AVAILABLE);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, 3L);
		verify(limitVerifier).getLimitsRemainingUnits(featureRef, userGrouping);
	}

	@Test
	void givenFeatureAllowedWhenGetUsageInfoThenReturnRemainingUnits() {
		String limitId = "ADD_USER";
		when(subscriptionVerifier.verifyEntitlement(featureRef, userGrouping)).thenReturn(SubscriptionVerifDetail.verificationOk());
		when(limitVerifier.getLimitsRemainingUnits(featureRef, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

		var featureInfo = featureUsageTracker.getUsageInfo(featureRef, userGrouping);

		assertThat(featureInfo.featureStatus()).isEqualTo(FeatureStatus.AVAILABLE);
		assertThat(featureInfo.remainingUsageUnits()).containsEntry(limitId, 1L);
	}

}
