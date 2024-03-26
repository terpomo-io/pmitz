package io.terpomo.pmitz.core.limits.types;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalendarPeriodRateLimitTest {

	ZonedDateTime referenceDate = ZonedDateTime.of(2024, 2, 20, 10, 15, 33, 7000, ZoneId.of("UTC"));

	String limitId = "max-photos-month";

	@Test
	void getWindowStartShouldReturnFirstNanosecondInHourWhenPeriodicityIsHour() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.HOUR);

		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 2, 20, 10, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowStartShouldReturnFirstNanosecondInDayWhenPeriodicityIsDay() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.DAY);

		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 2, 20, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowStartShouldReturnFirstDayInWeekWhenPeriodicityIsWeek() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);

		//2024-02-20 falls on Tuesday, first day of week is Monday 2024-02-19
		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 2, 19, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowStartShouldReturnSameDayWhenPeriodicityIsWeekAndFirstDayInWeek() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);

		var mondayReferenceDate = referenceDate.minusDays(1); //Monday 2024-02-19
		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 2, 19, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowStartShouldReturnFirstDayInMonthWhenPeriodicityIsMonth() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.MONTH);

		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowStartShouldReturnJanuary1stWhenPeriodicityIsYear() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.YEAR);

		ZonedDateTime expectedStartTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertTrue(expectedStartTime.isEqual(rateLimit.getWindowStart(referenceDate).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInHourWhenPeriodicityIsHour() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.HOUR);

		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 2, 20, 10, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDate).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInDayWhenPeriodicityIsDay() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.DAY);

		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 2, 20, 23, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDate).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInLastDayOfWeekWhenPeriodicityIsWeek() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);

		//2024-02-20 falls on Tuesday, next last day of week (Sunday) is 2024-02-25
		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 2, 25, 23, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDate).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInSameDayWhenPeriodicityIsWeekAndLastDayOfWeek() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);

		var referenceDateOnSunday = referenceDate.plusDays(5); //2024-02-25

		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 2, 25, 23, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDateOnSunday).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInLastDayOfMonthWhenPeriodicityIsMonth() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.MONTH);

		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 2, 29, 23, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDate).get()));
	}

	@Test
	void getWindowEndShouldReturnLastNanosecondInDecember31WhenPeriodicityIsYear() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.YEAR);

		ZonedDateTime expectedEndTime = ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC"));

		assertTrue(expectedEndTime.isEqual(rateLimit.getWindowEnd(referenceDate).get()));
	}

}