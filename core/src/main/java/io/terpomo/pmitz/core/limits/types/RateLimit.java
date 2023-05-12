package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.concurrent.TimeUnit;

public class RateLimit extends UsageLimit {

    private int quota;

    private TimeUnit interval;
}
