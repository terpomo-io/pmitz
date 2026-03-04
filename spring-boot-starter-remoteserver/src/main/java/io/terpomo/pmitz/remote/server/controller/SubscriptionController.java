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

package io.terpomo.pmitz.remote.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

	private final SubscriptionRepository subscriptionRepository;

	public SubscriptionController(SubscriptionRepository subscriptionRepository) {
		this.subscriptionRepository = subscriptionRepository;
	}

	@PostMapping
	public ResponseEntity<Void> createSubscription(@RequestBody Subscription subscription) {
		if (subscriptionRepository.find(subscription.getSubscriptionId()).isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		subscriptionRepository.create(subscription);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{subscriptionId}")
	public ResponseEntity<Subscription> findSubscription(@PathVariable String subscriptionId) {
		return subscriptionRepository.find(subscriptionId)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PatchMapping("/{subscriptionId}/status")
	public ResponseEntity<Void> updateSubscriptionStatus(@PathVariable String subscriptionId,
			@RequestBody SubscriptionStatusUpdateRequest request) {
		if (subscriptionRepository.find(subscriptionId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		subscriptionRepository.updateStatus(subscriptionId, request.status());
		return ResponseEntity.ok().build();
	}
}
