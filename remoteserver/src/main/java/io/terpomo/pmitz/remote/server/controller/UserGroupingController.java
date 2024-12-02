package io.terpomo.pmitz.remote.server.controller;

import java.util.Collections;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class UserGroupingController {

	private final FeatureUsageTracker featureUsageTracker;

	public UserGroupingController(@Autowired(required = false) FeatureUsageTracker featureUsageTracker) {
		this.featureUsageTracker = featureUsageTracker;
	}

	@GetMapping("/users/{userGroupingId}/limits/{productId}/{featureId}")
	public FeatureUsageInfo userUsageInfo(@PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@GetMapping("/directory-groups/{userGroupingId}/limits/{productId}/{featureId}")
	public FeatureUsageInfo groupUsageInfo(@PathVariable String productId,
										  @PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@GetMapping("/subscriptions/{userGroupingId}/limits/{productId}/{featureId}")
	public FeatureUsageInfo subscriptionUsageInfo(@PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@PostMapping("/users/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceUserFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {

		return ResponseEntity.ok().build();

	}

	@PostMapping("/directory-groups/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceGroupFeatureUsage(UsageRecordRequest usageRecordRequest,
															   @PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {

		return ResponseEntity.ok().build();
	}

	@PostMapping("/subscriptions/{userGroupingId}/usage/{productId}/{featureId}")
	public ResponseEntity<Void> recordOrReduceSubscriptionFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {

		return ResponseEntity.ok().build();

	}

	@PostMapping("/users/{userGroupingId}/usage-check/{productId}/{featureId}")
	public FeatureUsageInfo verifyUserFeatureUsage(UsageRecordRequest usageRecordRequest, @PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {

		return new FeatureUsageInfo(FeatureStatus.LIMIT_EXCEEDED, Collections.emptyMap());
	}

	@PostMapping("/directory-groups/{userGroupingId}/usage-check/{productId}/{featureId}")
	public FeatureUsageInfo verifyGroupFeatureUsage(UsageRecordRequest usageRecordRequest, @PathVariable String productId,
												   @PathVariable String featureId, @PathVariable String userGroupingId) {

		return new FeatureUsageInfo(FeatureStatus.LIMIT_EXCEEDED, Collections.emptyMap());
	}

	@PostMapping("/subscriptions/{userGroupingId}/usage-check/{productId}/{featureId}")
	public FeatureUsageInfo verifySubscriptionFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

}
