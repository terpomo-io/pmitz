package io.terpomo.pmitz.limits.impl.strategy;

import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategy;
import io.terpomo.pmitz.limits.UsageLimitVerificationStrategyResolver;

public class UsageLimitVerificationStrategyDefaultResolver implements UsageLimitVerificationStrategyResolver {

    private final UsageLimitVerificationStrategy defaultVerificationStrategy;

    public UsageLimitVerificationStrategyDefaultResolver() {
        defaultVerificationStrategy = new SimpleUsageLimitVerificationStrategy<>();
    }

    public UsageLimitVerificationStrategyDefaultResolver(UsageLimitVerificationStrategy defaultVerificationStrategy) {
        this.defaultVerificationStrategy = defaultVerificationStrategy;
    }

    @Override
    public <T extends UsageLimit> UsageLimitVerificationStrategy<T> resolveLimitVerificationStrategy(T usageLimit) {
        return defaultVerificationStrategy;
    }
}
