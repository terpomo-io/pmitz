package io.terpomo.pmitz.core.limits.types;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.*;
import java.util.Optional;

public class CalendarPeriodRateLimit extends RateLimit{

    private final Periodicity periodicity;

    public CalendarPeriodRateLimit(String id, int quota, Periodicity periodicity){
        super (id, quota, periodicity.getChronoUnit(), 1);
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
            case WEEK -> referenceDate.with(TemporalAdjusters.previousOrSame(getFirstDayOfWeek())).truncatedTo(ChronoUnit.DAYS);
            case MONTH -> referenceDate.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
            case YEAR -> referenceDate.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
        };

        return Optional.of(adjustedDateTime);
    }

    @Override
    public Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate) {
        ZonedDateTime adjustedDateTime = switch (periodicity) {
            case HOUR -> referenceDate.plusHours(1).truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.NANOS);
            case DAY -> lastNanosecondOfDay(referenceDate);
            case WEEK -> lastNanosecondOfDay(referenceDate.with(TemporalAdjusters.nextOrSame(getLastDayOfWeek())));
            case MONTH -> lastNanosecondOfDay(referenceDate.with(TemporalAdjusters.lastDayOfMonth()));
            case YEAR -> lastNanosecondOfDay(referenceDate.with(TemporalAdjusters.lastDayOfYear()));
        };

        return Optional.of(adjustedDateTime);
    }

    protected DayOfWeek getFirstDayOfWeek() {
        return DayOfWeek.MONDAY;
    }

    protected DayOfWeek getLastDayOfWeek() {
        return DayOfWeek.SUNDAY;
    }

    private ZonedDateTime lastNanosecondOfDay (ZonedDateTime dateTime) {
        return dateTime.with(ChronoField.SECOND_OF_DAY, (3600 * 24) -1)
                .with(ChronoField.NANO_OF_SECOND, 999_999_999);
    }

    public enum Periodicity {
        HOUR (ChronoUnit.HOURS),
        DAY (ChronoUnit.DAYS),
        WEEK (ChronoUnit.WEEKS),
        MONTH (ChronoUnit.MONTHS),
        YEAR (ChronoUnit.YEARS);

        private final ChronoUnit chronoUnit;

        Periodicity (ChronoUnit chronoUnit) {
            this.chronoUnit = chronoUnit;
        }

        ChronoUnit getChronoUnit() {
            return chronoUnit;
        }
    }
}
