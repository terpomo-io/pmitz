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

package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.time.temporal.ChronoUnit;

public abstract class RateLimit extends UsageLimit {

    private final long quota;

    private final ChronoUnit interval;

    private final int duration;

    RateLimit(String id, int quota, ChronoUnit interval, int duration) {
        super(id);
        this.quota = quota;
        this.interval = interval;
        this.duration = duration;
    }

    @Override
    public long getValue() {
        return quota;
    }

    public ChronoUnit getInterval() {
        return interval;
    }

    public int getDuration() {
        return duration;
    }
}
