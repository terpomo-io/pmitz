package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.limits.UsageLimit;

public interface UsageLimitVerificationStrategyResolver {

    <T extends UsageLimit> UsageLimitVerificationStrategy<T> resolveLimitVerificationStrategy (T usageLimit);
}
