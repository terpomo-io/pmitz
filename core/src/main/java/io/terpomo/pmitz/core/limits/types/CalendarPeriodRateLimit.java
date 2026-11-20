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

package io.terpomo.pmitz.core.limits.types;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public class CalendarPeriodRateLimit extends RateLimit {

	private final Periodicity periodicity;

	public CalendarPeriodRateLimit(String id, long quota, Periodicity periodicity) {
		super(id, quota, periodicity.getChronoUnit(), 1);
		this.periodicity = periodicity;
	}

	public Periodicity getPeriodicity() {
		return periodicity;
	}

	@Override
	public Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate) {
		ZonedDateTime adjustedDateTime = switch (periodicity) {
			case HOUR -> referenceDate.truncatedTo(ChronoUnit.HOURS);
			case DAY -> referenceDate.truncatedTo(ChronoUnit.DAYS);
			case WEEK ->
					referenceDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek())).truncatedTo(ChronoUnit.DAYS);
			case MONTH -> referenceDate.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
			case YEAR -> referenceDate.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
		};

		return Optional.of(adjustedDateTime);
	}

	/**
	 * Returns the end of the calendar period that contains the reference date. The end is exclusive: it is the
	 * start of the next period, so it is a whole date-time that any repository can store exactly.
	 */
	@Override
	public Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate) {
		return getWindowStart(referenceDate).map(windowStart -> windowStart.plus(1, periodicity.getChronoUnit()));
	}

	protected DayOfWeek getFirstDayOfWeek() {
		return DayOfWeek.MONDAY;
	}

	public enum Periodicity {
		HOUR(ChronoUnit.HOURS),
		DAY(ChronoUnit.DAYS),
		WEEK(ChronoUnit.WEEKS),
		MONTH(ChronoUnit.MONTHS),
		YEAR(ChronoUnit.YEARS);

		private final ChronoUnit chronoUnit;

		Periodicity(ChronoUnit chronoUnit) {
			this.chronoUnit = chronoUnit;
		}

		ChronoUnit getChronoUnit() {
			return chronoUnit;
		}
	}
}
