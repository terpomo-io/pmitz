package io.terpomo.pmitz.limits.impl.strategy;

import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategyResolver;

public class UsageLimitVerificationStrategyDefaultResolver implements UsageLimitVerificationStrategyResolver {

    private final UsageLimitVerificationStrategy defaultVerificationStrategy = new SimpleUsageLimitVerificationStrategy<>();

    @Override
    public <T extends UsageLimit> UsageLimitVerificationStrategy<T> resolveLimitVerificationStrategy(T usageLimit) {
        return defaultVerificationStrategy;
    }
}
