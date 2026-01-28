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

package io.terpomo.pmitz.subscriptions;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionVerifierImplTests {

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private SubscriptionFeatureManager subscriptionFeatureManager;

	@Mock
	private Subscription subscription;

	private SubscriptionVerifierImpl subscriptionVerifier;

	private Product product;
	private Feature feature;

	@BeforeEach
	void setUp() {
		subscriptionVerifier = new SubscriptionVerifierImpl(subscriptionRepository, subscriptionFeatureManager);
		product = new Product("test-product");
		feature = new Feature(product, "test-feature");
	}

	@Test
	void verifyEntitlementShouldReturnInvalidSubscriptionErrorWhenSubscriptionNotFound() {
		when(subscriptionRepository.find("user-123")).thenReturn(Optional.empty());
		when(subscription.getId()).thenReturn("user-123");

		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, subscription);

		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION);
	}

	@Test
	void verifyEntitlementShouldReturnInvalidSubscriptionErrorWhenSubscriptionNotValid() {
		when(subscriptionRepository.find("user-123")).thenReturn(Optional.of(subscription));
		when(subscription.getId()).thenReturn("user-123");
		when(subscription.isValid()).thenReturn(false);

		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, subscription);

		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.INVALID_SUBSCRIPTION);
	}

	@Test
	void verifyEntitlementShouldReturnProductNotAllowedErrorWhenProductNotInSubscription() {
		when(subscriptionRepository.find("user-123")).thenReturn(Optional.of(subscription));
		when(subscription.getId()).thenReturn("user-123");
		when(subscription.isValid()).thenReturn(true);
		when(subscription.isProductAllowed("test-product")).thenReturn(false);

		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, subscription);

		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.PRODUCT_NOT_ALLOWED);
	}

	@Test
	void verifyEntitlementShouldReturnFeatureNotAllowedErrorWhenFeatureNotIncluded() {
		when(subscriptionRepository.find("user-123")).thenReturn(Optional.of(subscription));
		when(subscription.getId()).thenReturn("user-123");
		when(subscription.isValid()).thenReturn(true);
		when(subscription.isProductAllowed("test-product")).thenReturn(true);
		when(subscriptionFeatureManager.isFeatureIncluded(subscription, feature)).thenReturn(false);

		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, subscription);

		assertThat(result.isFeatureAllowed()).isFalse();
		assertThat(result.getErrorCause()).isEqualTo(SubscriptionVerifDetail.ErrorCause.FEATURE_NOT_ALLOWED);
	}

	@Test
	void verifyEntitlementShouldReturnVerificationOkWhenAllConditionsMet() {
		when(subscriptionRepository.find("user-123")).thenReturn(Optional.of(subscription));
		when(subscription.getId()).thenReturn("user-123");
		when(subscription.isValid()).thenReturn(true);
		when(subscription.isProductAllowed("test-product")).thenReturn(true);
		when(subscriptionFeatureManager.isFeatureIncluded(subscription, feature)).thenReturn(true);

		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, subscription);

		assertThat(result.isFeatureAllowed()).isTrue();
		assertThat(result.getErrorCause()).isNull();
	}

	@Test
	void verifyEntitlementShouldAllowNonSubscriptionGroupings() {
		SubscriptionVerifDetail result = subscriptionVerifier.verifyEntitlement(feature, new IndividualUser("user-123"));

		assertThat(result.isFeatureAllowed()).isTrue();
		assertThat(result.getErrorCause()).isNull();
	}
}
