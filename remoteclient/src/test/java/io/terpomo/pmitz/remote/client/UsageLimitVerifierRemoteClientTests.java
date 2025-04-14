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

package io.terpomo.pmitz.remote.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierRemoteClientTests {

	@Mock
	PmitzClient pmitzClient;

	UsageLimitVerifierRemoteClient usageLimitVerifierRemoteClient;

	Feature feature;
	UserGrouping userGrouping;

	@BeforeEach
	void setUp() {
		userGrouping = new IndividualUser("user001");
		feature = new Feature(new Product("productId"), "featureId");
		usageLimitVerifierRemoteClient = new UsageLimitVerifierRemoteClient("url", pmitzClient);
	}

	@Test
	void getLimitsRemainingUnitsShouldCallPmitzClient() {
		var expectedRemainingUnits = Map.of("limit1", 10L);
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.getLimitsRemainingUnits(feature, userGrouping)).thenReturn(featureUsageInfo);

		var returnedRemainingUnits = usageLimitVerifierRemoteClient.getLimitsRemainingUnits(feature, userGrouping);

		assertThat(returnedRemainingUnits).isSameAs(expectedRemainingUnits);
		verify(pmitzClient).getLimitsRemainingUnits(feature, userGrouping);
	}

	@Test
	void isWithinLimitsShouldReturnTrueWhenLimitsNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		var expectedRemainingUnits = Map.of("limit1", 0L); //zero remainingUnit is Ok in this case
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.verifyLimits(feature, userGrouping, additionalUnits)).thenReturn(featureUsageInfo);

		var isWithinLimits = usageLimitVerifierRemoteClient.isWithinLimits(feature, userGrouping, additionalUnits);

		assertThat(isWithinLimits).isTrue();
		verify(pmitzClient).verifyLimits(feature, userGrouping, additionalUnits);
	}

	@Test
	void isWithinLimitsShouldReturnFalseWhenLimitsNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		var expectedRemainingUnits = Map.of("limit1", -2L); //zero remainingUnit is Ok in this case
		var featureUsageInfo = new FeatureUsageInfo(FeatureStatus.AVAILABLE, expectedRemainingUnits);
		when(pmitzClient.verifyLimits(feature, userGrouping, additionalUnits)).thenReturn(featureUsageInfo);

		var isWithinLimits = usageLimitVerifierRemoteClient.isWithinLimits(feature, userGrouping, additionalUnits);

		assertThat(isWithinLimits).isFalse();
		verify(pmitzClient).verifyLimits(feature, userGrouping, additionalUnits);
	}

	@Test
	void recordFeatureUsageShouldNotThrowExceptionWhenLimitNotExceeded() {
		var additionalUnits = Map.of("limit1", 10L);

		usageLimitVerifierRemoteClient.recordFeatureUsage(feature, userGrouping, additionalUnits);

		verify(pmitzClient).recordOrReduce(feature, userGrouping, additionalUnits, false);
	}

	@Test
	void recordFeatureUsageShouldThrowLimitExceededExceptionWhenLimitExceeded() {
		var additionalUnits = Map.of("limit1", 10L);
		doThrow(new LimitExceededException("Limit exceeded", feature, userGrouping))
				.when(pmitzClient).recordOrReduce(feature, userGrouping, additionalUnits, false);

		assertThatThrownBy(() -> usageLimitVerifierRemoteClient.recordFeatureUsage(feature, userGrouping, additionalUnits))
				.isInstanceOf(LimitExceededException.class);

		verify(pmitzClient).recordOrReduce(feature, userGrouping, additionalUnits, false);
	}

	@Test
	void reduceFeatureUsageShouldCallPmitzClient() {
		var additionalUnits = Map.of("limit1", 10L);

		usageLimitVerifierRemoteClient.reduceFeatureUsage(feature, userGrouping, additionalUnits);

		verify(pmitzClient).recordOrReduce(feature, userGrouping, additionalUnits, true);
	}

	@Test
	void updateProductShouldCallPmitzClient() {
		var inputStream = new ByteArrayInputStream("Content of product File".getBytes(StandardCharsets.UTF_8));
		usageLimitVerifierRemoteClient.uploadProduct(inputStream);

		verify(pmitzClient).uploadProduct(inputStream);
	}
}
