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

package io.terpomo.pmitz.limits;

import java.time.ZonedDateTime;

import io.terpomo.pmitz.limits.usage.repository.UsageRecordRepoMetadata;

public record UsageRecord(UsageRecordRepoMetadata repoMetadata, String limitId, ZonedDateTime startTime,
		ZonedDateTime endTime, Long units, ZonedDateTime expirationDate) {

	public UsageRecord(String limitId, ZonedDateTime startTime, ZonedDateTime endTime, Long units, ZonedDateTime expirationDate) {
		this(null, limitId, startTime, endTime, units, expirationDate);
	}

	public static UsageRecord updage(UsageRecord usageRecord, Long units, ZonedDateTime expirationDate) {
		return new UsageRecord(usageRecord.repoMetadata(), usageRecord.limitId(), usageRecord.startTime, usageRecord.endTime, units, expirationDate);
	}

}
