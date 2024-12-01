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
@RequestMapping("/products")
public class PmitzServerController {

	private final FeatureUsageTracker featureUsageTracker;

	public PmitzServerController(@Autowired(required = false) FeatureUsageTracker featureUsageTracker) {
		this.featureUsageTracker = featureUsageTracker;
	}

	@GetMapping("/hello/{productId}/{featureId}")
	public FeatureUsageInfo hello(@PathVariable String productId,
			@PathVariable String featureId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@GetMapping("/{productId}/features/{featureId}/usage/user/{userGroupingId}")
	public FeatureUsageInfo userUsageInfo(@PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@GetMapping("/{productId}/features/{featureId}/usage/subscription/{userGroupingId}")
	public FeatureUsageInfo subscriptionUsageInfo(@PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

	@PostMapping("/{productId}/features/{featureId}/usage/user/{userGroupingId}")
	public ResponseEntity<Void> recordOrReduceUserFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {

		return ResponseEntity.ok().build();

	}

	@PostMapping("/{productId}/features/{featureId}/usage/subscription/{userGroupingId}")
	public ResponseEntity<Void> recordOrReduceSubscriptionFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {

		return ResponseEntity.ok().build();

	}

	@PostMapping("/{productId}/features/{featureId}/usage/user/{userGroupingId}/verify")
	public FeatureUsageInfo verifyUserFeatureUsage(UsageRecordRequest usageRecordRequest, @PathVariable String productId,
			@PathVariable String featureId, @PathVariable String userGroupingId) {

		return new FeatureUsageInfo(FeatureStatus.LIMIT_EXCEEDED, Collections.emptyMap());
	}

	@PostMapping("/{productId}/features/{featureId}/usage/subscription/{userGroupingId}/verify")
	public FeatureUsageInfo verifySubscriptionFeatureUsage(UsageRecordRequest usageRecordRequest,
			@PathVariable String productId, @PathVariable String featureId, @PathVariable String userGroupingId) {
		return new FeatureUsageInfo(FeatureStatus.AVAILABLE, Collections.emptyMap());
	}

}
