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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionVerifierRemoteClientTests {

	@Mock
	PmitzClient pmitzClient;

	SubscriptionVerifierRemoteClient subscriptionVerifierRemoteClient;

	FeatureRef featureRef;

	@BeforeEach
	void setUp() {
		featureRef = new FeatureRef("productId", "featureId");
		subscriptionVerifierRemoteClient = new SubscriptionVerifierRemoteClient(pmitzClient);
	}

	@Test
	void verifyEntitlementShouldReturnSuccessWhenFeatureAllowed() {
		UserGrouping userGrouping = new IndividualUser("user001");
		var expectedVerifDetail = SubscriptionVerifDetail.verificationOk();
		when(pmitzClient.verifySubscription(featureRef, userGrouping)).thenReturn(expectedVerifDetail);

		var result = subscriptionVerifierRemoteClient.verifyEntitlement(featureRef, userGrouping);

		assertThat(result).isSameAs(expectedVerifDetail);
		assertThat(result.isFeatureAllowed()).isTrue();
		assertThat(result.getErrorCause()).isNull();
		verify(pmitzClient).verifySubscription(featureRef, userGrouping);
	}

	@Test
	void verifyEntitlementShouldReturnErrorWhenInvalidSubscription() {
		UserGrouping userGrouping = new Subscription("sub001");
		var expectedVerifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION);
		when(pmitzClient.verifySubscription(featureRef, userGrouping)).thenReturn(expectedVerifDetail);

		var result = subscriptionVerifierRemoteClient.verifyEntitlement(featureRef, userGrouping);

		assertThat(result).isSameAs(expectedVerifDetail);
		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION);
		verify(pmitzClient).verifySubscription(featureRef, userGrouping);
	}

	@Test
	void verifyEntitlementShouldReturnErrorWhenProductNotAllowed() {
		UserGrouping userGrouping = new Subscription("sub001");
		var expectedVerifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.PRODUCT_NOT_ALLOWED);
		when(pmitzClient.verifySubscription(featureRef, userGrouping)).thenReturn(expectedVerifDetail);

		var result = subscriptionVerifierRemoteClient.verifyEntitlement(featureRef, userGrouping);

		assertThat(result).isSameAs(expectedVerifDetail);
		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.PRODUCT_NOT_ALLOWED);
		verify(pmitzClient).verifySubscription(featureRef, userGrouping);
	}

	@Test
	void verifyEntitlementShouldReturnErrorWhenFeatureNotAllowed() {
		UserGrouping userGrouping = new Subscription("sub001");
		var expectedVerifDetail = SubscriptionVerifDetail.verificationError(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED);
		when(pmitzClient.verifySubscription(featureRef, userGrouping)).thenReturn(expectedVerifDetail);

		var result = subscriptionVerifierRemoteClient.verifyEntitlement(featureRef, userGrouping);

		assertThat(result).isSameAs(expectedVerifDetail);
		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED);
		verify(pmitzClient).verifySubscription(featureRef, userGrouping);
	}

	@Test
	void verifyEntitlementShouldPropagateExceptionFromPmitzClient() {
		UserGrouping userGrouping = new IndividualUser("user001");
		when(pmitzClient.verifySubscription(featureRef, userGrouping))
				.thenThrow(new RemoteCallException("Connection failed"));

		assertThatThrownBy(() -> subscriptionVerifierRemoteClient.verifyEntitlement(featureRef, userGrouping))
				.isInstanceOf(RemoteCallException.class)
				.hasMessage("Connection failed");

		verify(pmitzClient).verifySubscription(featureRef, userGrouping);
	}
}
