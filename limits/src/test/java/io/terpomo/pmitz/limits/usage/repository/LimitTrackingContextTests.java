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

package io.terpomo.pmitz.limits.usage.repository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageRecord;

import static org.assertj.core.api.Assertions.assertThat;

class LimitTrackingContextTests {

	Product product = new Product("photo-sharing");
	Feature feature = new Feature(product, "upload-photo");

	String limitId = "number-of-photos";

	@Test
	void addUsageRecordsShouldAddRecordsToUpdatedList() {
		LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());

		UsageRecord usageRecord = new UsageRecord(limitId, null, null, 3L, null);
		context.addUpdatedUsageRecords(Collections.singletonList(usageRecord));

		var updatedRecords = context.getUpdatedUsageRecords();
		assertThat(updatedRecords.size()).isEqualTo(1);
	}

	@Test
	void findUsageRecordsShouldFilterRecords() {
		ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
		ZonedDateTime endTime = ZonedDateTime.now();

		var usageRecords = getCurrentUsageRecords(startTime, endTime);

		LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());
		context.addCurrentUsageRecords(usageRecords);

		var filteredRecords = context.findUsageRecords(limitId, startTime, endTime);

		assertThat(filteredRecords.size()).isEqualTo(3);
	}

	@Test
	void findUsageRecordsShouldFilterRecordsWhenParamsEmpty() {
		ZonedDateTime startTime = ZonedDateTime.now().minusMonths(1);
		ZonedDateTime endTime = ZonedDateTime.now();

		var usageRecords = getCurrentUsageRecords(startTime, endTime);

		LimitTrackingContext context = new LimitTrackingContext(feature, new IndividualUser("user002"), Collections.emptyList());
		context.addCurrentUsageRecords(usageRecords);

		var filteredRecords = context.findUsageRecords(limitId, null, null);

		assertThat(filteredRecords.size()).isEqualTo(4);
	}

	private List<UsageRecord> getCurrentUsageRecords(ZonedDateTime startTime, ZonedDateTime endTime) {
		UsageRecord usageRecord1 = new UsageRecord(limitId, null, null, 3L, null);
		UsageRecord usageRecord2 = new UsageRecord(limitId, null, null, 2L, null);
		UsageRecord usageRecord3 = new UsageRecord(limitId, null, null, 2L, null);
		UsageRecord usageRecord4 = new UsageRecord(limitId, startTime.minusHours(1), endTime, 2L, null);

		return Arrays.asList(usageRecord1, usageRecord2, usageRecord3, usageRecord4);
	}
}
