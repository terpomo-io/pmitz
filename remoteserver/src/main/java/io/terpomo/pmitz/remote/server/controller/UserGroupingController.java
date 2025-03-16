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

package io.terpomo.pmitz.remote.server.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subjects.DirectoryGroup;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.limits.impl.LimitsValidationUtil;

@RestController
public class UserGroupingController {

	private final FeatureUsageTracker featureUsageTracker;
	private final ProductRepository productRepository;

	public UserGroupingController(FeatureUsageTracker featureUsageTracker, ProductRepository productRepository) {
		this.featureUsageTracker = featureUsageTracker;
		this.productRepository = productRepository;
	}

	@GetMapping("/users/{userGroupingId}/usage/{productId}/{featureId}")
	public FeatureUsageInfo verifyUserFeatureUsage(@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {

		Feature feature = findFeature(productId, featureId);
		UserGrouping userGrouping = new IndividualUser(userGroupingId);

		return featureUsageTracker.getUsageInfo(feature, userGrouping);
	}

	@GetMapping("/directory-groups/{userGroupingId}/usage/{productId}/{featureId}")
	public FeatureUsageInfo verifyGroupFeatureUsage(@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {

		Feature feature = findFeature(productId, featureId);
		UserGrouping userGrouping = new DirectoryGroup(userGroupingId);

		return featureUsageTracker.getUsageInfo(feature, userGrouping);
	}

	@GetMapping("/subscriptions/{userGroupingId}/usage/{productId}/{featureId}")
	public FeatureUsageInfo verifySubscriptionFeatureUsage(@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		UserGrouping userGrouping = new Subscription(userGroupingId);

		return featureUsageTracker.getUsageInfo(feature, userGrouping);
	}

	@PostMapping("/users/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceUserFeatureUsage(@RequestBody UsageRecordRequest usageRecordRequest,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		UserGrouping userGrouping = new IndividualUser(userGroupingId);

		return recordOrReduceFeatureUsage(feature, userGrouping, usageRecordRequest);
	}

	@PostMapping("/directory-groups/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceGroupFeatureUsage(@RequestBody UsageRecordRequest usageRecordRequest,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		UserGrouping userGrouping = new DirectoryGroup(userGroupingId);

		return recordOrReduceFeatureUsage(feature, userGrouping, usageRecordRequest);
	}

	@PostMapping("/subscriptions/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceSubscriptionFeatureUsage(@RequestBody UsageRecordRequest usageRecordRequest,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		Subscription userGrouping = new Subscription(userGroupingId);

		return recordOrReduceFeatureUsage(feature, userGrouping, usageRecordRequest);
	}

	@PostMapping("/users/{userGroupingId}/limits-check/{productId}/{featureId}")
	public FeatureUsageInfo verifyUserLimits(@RequestBody Map<String, Long> additionalUnits,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		return featureUsageTracker.verifyLimits(feature, new IndividualUser(userGroupingId), additionalUnits);
	}

	@PostMapping("/directory-groups/{userGroupingId}/limits-check/{productId}/{featureId}")
	public FeatureUsageInfo verifyGroupLimits(@RequestBody Map<String, Long> additionalUnits,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		return featureUsageTracker.verifyLimits(feature, new DirectoryGroup(userGroupingId), additionalUnits);
	}

	@PostMapping("/subscriptions/{userGroupingId}/limits-check/{productId}/{featureId}")
	public FeatureUsageInfo verifySubscriptionLimits(@RequestBody Map<String, Long> additionalUnits,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);
		return featureUsageTracker.verifyLimits(feature, new Subscription(userGroupingId), additionalUnits);
	}

	private Feature findFeature(String productId, String featureId) {
		var product = productRepository.getProductById(productId).orElseThrow(() -> new IllegalArgumentException("Invalid product"));
		return productRepository.getFeature(product, featureId).orElseThrow(() -> new IllegalArgumentException("Invalid feature"));
	}

	private ResponseEntity<Void> recordOrReduceFeatureUsage(Feature feature, UserGrouping userGrouping, UsageRecordRequest usageRecordRequest) {
		try {
			LimitsValidationUtil.validateAdditionalUnits(usageRecordRequest.getUnits());
		}
		catch (IllegalArgumentException exception) {
			ResponseEntity.status(400).body(exception.getMessage());
		}

		if (usageRecordRequest.isReduceUnits()) {
			featureUsageTracker.reduceFeatureUsage(feature, userGrouping, usageRecordRequest.getUnits());
		}
		else {
			try {
				featureUsageTracker.recordFeatureUsage(feature, userGrouping, usageRecordRequest.getUnits());
			}
			catch (LimitExceededException exception) {
				return ResponseEntity.status(422).build();
			}
		}
		return ResponseEntity.ok().build();
	}
}
