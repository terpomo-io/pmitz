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

public class UsageRecordRequest {

	private boolean reduceUnits;
	private Map<String, Long> units;

	public boolean isReduceUnits() {
		return reduceUnits;
	}

	public void setReduceUnits(boolean reduceUnits) {
		this.reduceUnits = reduceUnits;
	}

	public Map<String, Long> getUnits() {
		return units;
	}

	public void setUnits(Map<String, Long> units) {
		this.units = units;
	}
}
