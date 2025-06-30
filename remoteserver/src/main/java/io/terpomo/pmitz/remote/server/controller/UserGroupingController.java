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

	@GetMapping("/{userGroupingType}/{userGroupingId}/usage/{productId}/{featureId}")
	public FeatureUsageInfo verifyUserFeatureUsage(@PathVariable String userGroupingType, @PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {

		Feature feature = findFeature(productId, featureId);

		UserGrouping userGrouping = resolveUserGrouping(userGroupingType, userGroupingId);

		return featureUsageTracker.getUsageInfo(feature, userGrouping);
	}

	@PostMapping("/{userGroupingType}/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceUserFeatureUsage(@PathVariable String userGroupingType, @RequestBody UsageRecordRequest usageRecordRequest,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);

		UserGrouping userGrouping = resolveUserGrouping(userGroupingType, userGroupingId);

		return recordOrReduceFeatureUsage(feature, userGrouping, usageRecordRequest);
	}

	@PostMapping("/{userGroupingType}/{userGroupingId}/limits-check/{productId}/{featureId}")
	public FeatureUsageInfo verifyUserLimits(@PathVariable String userGroupingType, @RequestBody Map<String, Long> additionalUnits,
			@PathVariable String productId,
			@PathVariable String featureId,
			@PathVariable String userGroupingId) {
		Feature feature = findFeature(productId, featureId);

		UserGrouping userGrouping = resolveUserGrouping(userGroupingType, userGroupingId);

		return featureUsageTracker.verifyLimits(feature, userGrouping, additionalUnits);
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

	private UserGrouping resolveUserGrouping(String userGroupingType, String userGroupingId) {
		UserGrouping userGrouping = switch (userGroupingType) {
			case "users" -> new IndividualUser(userGroupingId);
			case "subscriptions" -> new Subscription(userGroupingId);
			case "directory-groups" -> new DirectoryGroup(userGroupingId);
			default -> null;
		};
		if (userGrouping == null){
			throw new IllegalArgumentException("Invalid grouping type : Valid values must ne one of users, " +
					"subscriptions and directory-groups");
		}
		return userGrouping;
	}
}
