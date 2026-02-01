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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSubscriptionFeatureManagerTests {

	@Mock
	Subscription subscription;

	@Mock
	Feature feature;

	@Mock
	Plan plan;

	@Mock
	ProductRepository productRepository;

	private final String productId = "PhotoSharing";
	private final String featureId = "featureId";

	@Test
	void isFeatureIncludedShouldReturnFalseWhenProductNotAllowed() {
		FeatureRef featureRef = new FeatureRef(productId, featureId);

		when(subscription.isProductAllowed(productId)).thenReturn(false);

		DefaultSubscriptionFeatureManager subscriptionFeatureManager = new DefaultSubscriptionFeatureManager(productRepository);
		assertThat(subscriptionFeatureManager.isFeatureIncluded(subscription, featureRef)).isFalse();
	}

	@Test
	void isFeatureIncludedShouldReturnFalseWhenNoPlanForProduct() {
		FeatureRef featureRef = new FeatureRef(productId, featureId);

		when(subscription.isProductAllowed(productId)).thenReturn(true);
		when(subscription.getPlan(productId)).thenReturn(Optional.empty());

		DefaultSubscriptionFeatureManager subscriptionFeatureManager = new DefaultSubscriptionFeatureManager(productRepository);
		assertThat(subscriptionFeatureManager.isFeatureIncluded(subscription, featureRef)).isFalse();
	}

	@Test
	void isFeatureIncludedShouldReturnTrueWhenFeatureAllowed() {
		String planId = "planId";
		FeatureRef featureRef = new FeatureRef(productId, featureId);
		Product product = new Product(productId);

		when(subscription.isProductAllowed(productId)).thenReturn(true);
		when(subscription.getPlan(productId)).thenReturn(Optional.of(planId));

		when(productRepository.getProductById(productId)).thenReturn(Optional.of(product));
		when(productRepository.getPlan(product, planId)).thenReturn(Optional.of(plan));
		when(plan.getIncludedFeature(featureId)).thenReturn(Optional.of(feature));

		DefaultSubscriptionFeatureManager subscriptionFeatureManager = new DefaultSubscriptionFeatureManager(productRepository);
		assertThat(subscriptionFeatureManager.isFeatureIncluded(subscription, featureRef)).isTrue();
	}

}
