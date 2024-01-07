package io.terpomo.pmitz.core.limits.types;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimitTest {

    SlidingWindowRateLimit rateLimit = new SlidingWindowRateLimit("photos-uploaded", 10, ChronoUnit.DAYS, 30);
    @Test
    void getWindowStart() {

        assertNotNull(rateLimit.getWindowStart(ZonedDateTime.now()));
    }

    @Test
    void getWindowEnd() {
    }
}