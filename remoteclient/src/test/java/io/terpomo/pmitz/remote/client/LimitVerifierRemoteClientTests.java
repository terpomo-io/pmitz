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

package io.terpomo.pmitz.remote.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitVerifierRemoteClientTests {

	@Mock
	PmitzClient pmitzClient;

	LimitVerifierRemoteClient limitVerifierRemoteClient;

	FeatureRef featureRef;
	UserGrouping userGrouping;

	@BeforeEach
	void setUp() {
		userGrouping = new IndividualUser("user001");
		featureRef = new FeatureRef("productId", "featureId");
		limitVerifierRemoteClient = new LimitVerifierRemoteClient(pmitzClient);
	}

	@Test
	void getLimitsRemainingUnitsShouldCallPmitzClient() {
		var expectedRemainingUnits = Map.of("limit1", 10L);
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.getLimitsRemainingUnits(featureRef, userGrouping)).thenReturn(featureUsageInfo);

		var returnedRemainingUnits = limitVerifierRemoteClient.getLimitsRemainingUnits(featureRef, userGrouping);

		assertThat(returnedRemainingUnits).isSameAs(expectedRemainingUnits);
		verify(pmitzClient).getLimitsRemainingUnits(featureRef, userGrouping);
	}

	@Test
	void isWithinLimitsShouldReturnTrueWhenLimitsNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		var expectedRemainingUnits = Map.of("limit1", 0L); //zero remainingUnit is Ok in this case
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.verifyLimits(featureRef, userGrouping, additionalUnits)).thenReturn(featureUsageInfo);

		var isWithinLimits = limitVerifierRemoteClient.isWithinLimits(featureRef, userGrouping, additionalUnits);

		assertThat(isWithinLimits).isTrue();
		verify(pmitzClient).verifyLimits(featureRef, userGrouping, additionalUnits);
	}

	@Test
	void isWithinLimitsShouldReturnFalseWhenLimitsNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		var expectedRemainingUnits = Map.of("limit1", -2L); //zero remainingUnit is Ok in this case
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.verifyLimits(featureRef, userGrouping, additionalUnits)).thenReturn(featureUsageInfo);

		var isWithinLimits = limitVerifierRemoteClient.isWithinLimits(featureRef, userGrouping, additionalUnits);

		assertThat(isWithinLimits).isFalse();
		verify(pmitzClient).verifyLimits(featureRef, userGrouping, additionalUnits);
	}

	@Test
	void recordFeatureUsageShouldNotThrowExceptionWhenLimitNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);

		limitVerifierRemoteClient.recordFeatureUsage(featureRef, userGrouping, additionalUnits);

		verify(pmitzClient).recordOrReduce(featureRef, userGrouping, additionalUnits, false);
	}

	@Test
	void recordFeatureUsageShouldThrowLimitExceededExceptionWhenLimitExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		doThrow(new LimitExceededException("Limit exceeded", featureRef, userGrouping))
				.when(pmitzClient).recordOrReduce(featureRef, userGrouping, additionalUnits, false);

		assertThatThrownBy(() -> limitVerifierRemoteClient.recordFeatureUsage(featureRef, userGrouping, additionalUnits))
				.isInstanceOf(LimitExceededException.class);

		verify(pmitzClient).recordOrReduce(featureRef, userGrouping, additionalUnits, false);
	}

	@Test
	void reduceFeatureUsageShouldCallPmitzClient() {
		var additionalUnits = Map.of("limit1", 10L);

		limitVerifierRemoteClient.reduceFeatureUsage(featureRef, userGrouping, additionalUnits);

		verify(pmitzClient).recordOrReduce(featureRef, userGrouping, additionalUnits, true);
	}

	@Test
	void updateProductShouldCallPmitzClient() {
		var inputStream = new ByteArrayInputStream("Content of product File".getBytes(StandardCharsets.UTF_8));
		limitVerifierRemoteClient.uploadProduct(inputStream);

		verify(pmitzClient).uploadProduct(inputStream);
	}
}
