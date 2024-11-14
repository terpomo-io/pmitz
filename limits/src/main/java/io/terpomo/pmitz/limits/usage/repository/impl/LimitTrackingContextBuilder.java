package io.terpomo.pmitz.limits.usage.repository.impl;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;

public class LimitTrackingContextBuilder {
	private String limitId;
	private ZonedDateTime windowStart;
	private ZonedDateTime windowEnd;
	private String userId;
	private String productId;
	private String featureId;
	private List<RecordSearchCriteria> searchCriteria;
	private List<UsageRecord> updatedUsageRecords;

	// Private constructor to prevent direct instantiation
	private LimitTrackingContextBuilder() {}

	// Static method to create a new builder instance
	public static LimitTrackingContextBuilder newInstance() {
		return new LimitTrackingContextBuilder();
	}

	// with* methods to set properties
	public LimitTrackingContextBuilder withLimitId(String limitId) {
		this.limitId = limitId;
		return this;
	}

	public LimitTrackingContextBuilder withWindowStart(ZonedDateTime windowStart) {
		this.windowStart = windowStart;
		return this;
	}

	public LimitTrackingContextBuilder withWindowEnd(ZonedDateTime windowEnd) {
		this.windowEnd = windowEnd;
		return this;
	}

	public LimitTrackingContextBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}

	public LimitTrackingContextBuilder withProductId(String productId) {
		this.productId = productId;
		return this;
	}

	public LimitTrackingContextBuilder withFeatureId(String featureId) {
		this.featureId = featureId;
		return this;
	}

	public LimitTrackingContextBuilder withSearchCriteria(List<RecordSearchCriteria> searchCriteria) {
		this.searchCriteria = searchCriteria;
		return this;
	}

	public LimitTrackingContextBuilder withUpdatedUsageRecords(List<UsageRecord> updatedUsageRecords) {
		this.updatedUsageRecords = updatedUsageRecords;
		return this;
	}

	// Add a single RecordSearchCriteria to the list
	public LimitTrackingContextBuilder addSearchCriteria(RecordSearchCriteria searchCriteria) {
		if (this.searchCriteria == null) {
			this.searchCriteria = new ArrayList<>();
		}
		this.searchCriteria.add(searchCriteria);
		return this;
	}

	// Add a single UsageRecord to the list of updated records
	public LimitTrackingContextBuilder addUpdatedUsageRecord(UsageRecord updatedUsageRecord) {
		if (this.updatedUsageRecords == null) {
			this.updatedUsageRecords = new ArrayList<>();
		}
		this.updatedUsageRecords.add(updatedUsageRecord);
		return this;
	}

	// Build the LimitTrackingContext instance
	public LimitTrackingContext build() {
		if (userId == null || productId == null || featureId == null) {
			throw new IllegalStateException("User ID, Product ID, and Feature ID are required");
		}

		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return userId;
			}
		};

		Feature feature = new Feature(new Product(productId), featureId);

		List<RecordSearchCriteria> criteria = searchCriteria!= null? searchCriteria : new ArrayList<>();

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, criteria);

		if (updatedUsageRecords!= null) {
			context.addUpdatedUsageRecords(updatedUsageRecords);
		}

		return context;
	}
}