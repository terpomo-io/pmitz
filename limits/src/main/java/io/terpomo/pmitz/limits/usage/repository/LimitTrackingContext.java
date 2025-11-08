/*
 * Copyright 2023-2025 the original author or authors.
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

package io.terpomo.pmitz.limits.usage.repository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;

public class LimitTrackingContext {

	private final Feature feature;

	private final UserGrouping userGrouping;
	private final List<RecordSearchCriteria> searchCriteria;

	private final List<UsageRecord> currentUsageRecords;

	private final List<UsageRecord> updatedUsageRecords;

	public LimitTrackingContext(Feature feature, UserGrouping userGrouping, List<RecordSearchCriteria> searchCriteria) {
		this.feature = feature;
		this.userGrouping = userGrouping;
		this.searchCriteria = searchCriteria;
		this.currentUsageRecords = new ArrayList<>();
		this.updatedUsageRecords = new ArrayList<>();
	}

	public Feature getFeature() {
		return feature;
	}

	public UserGrouping getUserGrouping() {
		return userGrouping;
	}

	public List<RecordSearchCriteria> getSearchCriteria() {
		return searchCriteria;
	}

	public List<UsageRecord> getCurrentUsageRecords() {
		return currentUsageRecords;
	}

	public List<UsageRecord> getUpdatedUsageRecords() {
		return updatedUsageRecords;
	}

	public void addCurrentUsageRecords(List<UsageRecord> usageRecords) {
		currentUsageRecords.addAll(usageRecords);
	}

	public void addUpdatedUsageRecords(List<UsageRecord> additionalUsageRecords) {
		updatedUsageRecords.addAll(additionalUsageRecords);
	}

	public List<UsageRecord> findUsageRecords(String limitId, ZonedDateTime startTime, ZonedDateTime endTime) {

		Predicate<UsageRecord> filterCondition = usageRecord -> limitId.equals(usageRecord.limitId())
				&& (startTime == null || usageRecord.startTime() == null || usageRecord.startTime().isAfter(startTime) || usageRecord.startTime().isEqual(startTime))
				&& (endTime == null || usageRecord.endTime() == null || usageRecord.endTime().isBefore(endTime) || usageRecord.endTime().isEqual(endTime));

		return getCurrentUsageRecords().stream().filter(filterCondition).toList();
	}
}
