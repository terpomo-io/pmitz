package io.terpomo.pmitz.limits.impl.strategy;

import java.time.temporal.ChronoUnit;

import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategyResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsageLimitVerificationStrategyDefaultResolverTest {

	UsageLimitVerificationStrategyResolver strategyResolver = new UsageLimitVerificationStrategyDefaultResolver();
	private String limitId = "max-number-of-photos";
	;

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenCountLimit() {
		CountLimit countLimit = new CountLimit(limitId, 10);
		UsageLimitVerificationStrategy<CountLimit> countLimitVerifStrategy = strategyResolver.resolveLimitVerificationStrategy(countLimit);
		assertNotNull(countLimitVerifStrategy);
	}

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenCalendarPeriodRateLimit() {
		CalendarPeriodRateLimit rateLimit = new CalendarPeriodRateLimit(limitId, 10, CalendarPeriodRateLimit.Periodicity.WEEK);
		UsageLimitVerificationStrategy<CalendarPeriodRateLimit> verifStrategy = strategyResolver.resolveLimitVerificationStrategy(rateLimit);
		assertNotNull(verifStrategy);
	}

	@Test
	void resolveLimitVerificationStrategyShouldReturnSimpleStrategyWhenSlidingWindowRateLimit() {
		SlidingWindowRateLimit rateLimit = new SlidingWindowRateLimit(limitId, 10, ChronoUnit.MONTHS, 1);
		UsageLimitVerificationStrategy<SlidingWindowRateLimit> verifStrategy = strategyResolver.resolveLimitVerificationStrategy(rateLimit);
		assertNotNull(verifStrategy);
	}

}