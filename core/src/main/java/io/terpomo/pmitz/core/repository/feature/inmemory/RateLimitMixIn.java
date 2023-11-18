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

package io.terpomo.pmitz.core.repository.feature.inmemory;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@ExcludeFromJacocoGeneratedReport
public abstract class RateLimitMixIn {

	@JsonProperty("quota")
	private int quota;
	@JsonProperty("interval")
	private TimeUnit interval;
	@JsonProperty("duration")
	private int duration;

	@JsonCreator
	public RateLimitMixIn(
			@JsonProperty("quota") int quota,
			@JsonProperty("interval") TimeUnit interval,
			@JsonProperty("duration") int duration) {};

	@JsonIgnore
	public long getValue() {
		return 0;
	}
}
