package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.limits.UsageLimit;

public interface LimitVerificationStrategyResolver {

    <T extends UsageLimit> LimitVerificationStrategy<T> resolveLimitVerificationStrategy (T usageLimit);
}
