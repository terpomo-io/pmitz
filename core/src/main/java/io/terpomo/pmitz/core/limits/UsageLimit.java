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

package io.terpomo.pmitz.core.limits;

import java.time.ZonedDateTime;
import java.util.Optional;

public abstract class UsageLimit {

	private String unit;
	private String id;

	public UsageLimit(String id) {
		this.id = id;
	}

	public UsageLimit(String id, String unit) {
		this.id = id;
		this.unit = unit;
	}

	public abstract long getValue();

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public abstract Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate);

	public abstract Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate);
}
