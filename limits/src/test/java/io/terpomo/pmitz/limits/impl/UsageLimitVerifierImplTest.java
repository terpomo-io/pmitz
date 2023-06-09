package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.limits.UsageLimitResolver;
import io.terpomo.pmitz.limits.UsageCounter;
import org.junit.jupiter.api.Test;
import io.terpomo.pmitz.core.Feature;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierImplTest {

    private final Feature feature = new Feature();
    private final String limitId = "file uploads";

    @Mock
    private UsageLimitResolver usageResolver;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private UsageCounter usageCounter;

    @Test
    void givenUsageMoreThanLimitWhenIsLimitExceededThenReturnTrue (){
        ZonedDateTime endTime = ZonedDateTime.now();
        Optional<ZonedDateTime> startTime = Optional.of(endTime.minusDays(30));

        UsageLimit usageLimit  = mock(UsageLimit.class);//new RateLimit(10, TimeUnit.DAYS, 30);
        when (usageLimit.getValue()).thenReturn(10);
        when(usageLimit.getWindowStart(endTime)).thenReturn(startTime);

        UsageLimitVerifierImpl usageLimitVerifier = new UsageLimitVerifierImpl(usageResolver, usageCounter);

        UserGrouping subscription = new Subscription("sub001");
        when(usageCounter.getUsageCount(feature, limitId, subscription, startTime, endTime))
                .thenReturn (Optional.of(10));

        when(usageResolver.resolveUsageLimit(feature, limitId, subscription))
                .thenReturn(Optional.of(usageLimit));

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = Mockito.mockStatic(ZonedDateTime.class)) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(endTime);

            boolean withinLimit = usageLimitVerifier.isWithinLimit(feature, limitId, subscription, 1);
            assertFalse(withinLimit);
        }
    }
}