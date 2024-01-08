/*
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

package io.terpomo.pmitz.core.repository.product.inmemory;

import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;

@ExcludeFromJacocoGeneratedReport
public abstract class CalendarPeriodRateLimitMixIn {

	@JsonProperty("id")
	private String id;
	@JsonProperty("quota")
	private int quota;
	@JsonProperty("periodicity")
	private CalendarPeriodRateLimit.Periodicity periodicity;

	@JsonCreator
	public CalendarPeriodRateLimitMixIn(
			@JsonProperty("id") String id,
			@JsonProperty("quota") int quota,
			@JsonProperty("periodicity") CalendarPeriodRateLimit.Periodicity periodicity) { }

	@JsonIgnore
	public long getValue() {
		return 0;
	}
	@JsonIgnore
	public int getDuration() {
		return 0;
	}
	@JsonIgnore
	public ChronoUnit getInterval() {
		return null;
	}
}
