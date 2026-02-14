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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionRepoRemoteClientTests {

	@Mock
	PmitzClient pmitzClient;

	SubscriptionRepoRemoteClient subscriptionRepoRemoteClient;

	@BeforeEach
	void setUp() {
		subscriptionRepoRemoteClient = new SubscriptionRepoRemoteClient(pmitzClient);
	}

	@Test
	void createShouldCallPmitzClient() {
		var subscription = new Subscription("sub001");
		subscription.setStatus(SubscriptionStatus.ACTIVE);

		subscriptionRepoRemoteClient.create(subscription);

		verify(pmitzClient).createSubscription(subscription);
	}

	@Test
	void findShouldReturnSubscriptionWhenFound() {
		var expectedSubscription = new Subscription("sub001");
		expectedSubscription.setStatus(SubscriptionStatus.ACTIVE);
		when(pmitzClient.findSubscription("sub001")).thenReturn(Optional.of(expectedSubscription));

		var result = subscriptionRepoRemoteClient.find("sub001");

		assertThat(result).isPresent();
		assertThat(result).containsSame(expectedSubscription);
		verify(pmitzClient).findSubscription("sub001");
	}

	@Test
	void findShouldReturnEmptyWhenNotFound() {
		when(pmitzClient.findSubscription("sub001")).thenReturn(Optional.empty());

		var result = subscriptionRepoRemoteClient.find("sub001");

		assertThat(result).isEmpty();
		verify(pmitzClient).findSubscription("sub001");
	}

	@Test
	void updateStatusShouldCallPmitzClient() {
		subscriptionRepoRemoteClient.updateStatus("sub001", SubscriptionStatus.SUSPENDED);

		verify(pmitzClient).updateSubscriptionStatus("sub001", SubscriptionStatus.SUSPENDED);
	}

	@Test
	void updateStatusShouldPassSubscriptionIdToPmitzClient() {
		subscriptionRepoRemoteClient.updateStatus("customSubId", SubscriptionStatus.CANCELLED);

		verify(pmitzClient).updateSubscriptionStatus("customSubId", SubscriptionStatus.CANCELLED);
	}
}
