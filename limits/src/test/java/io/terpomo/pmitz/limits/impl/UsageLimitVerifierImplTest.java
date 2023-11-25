package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.LimitVerificationStrategy;
import io.terpomo.pmitz.limits.LimitVerificationStrategyResolver;
import io.terpomo.pmitz.limits.UsageLimitResolver;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierImplTest {

    @Mock
    UsageLimitResolver usageLimitResolver;

    @Mock
    UsageRepository usageRepo;

    @Mock
    LimitVerificationStrategy limitVerificationStrategy;

    @Mock
    LimitVerificationStrategyResolver limitVerificationStrategyResolver;

    @Mock
    LimitTrackingContext limitTrackingContext;

    Feature feature;

    UserGrouping userGrouping = new IndividualUser("user001");

    UsageLimit usageLimit = new CountLimit("MAX_FILES", 10L);

    ZonedDateTime zonedDateTime;

    UsageLimitVerifierImpl usageLimitVerifier;

    @BeforeEach
    void init() {
        Product product = new Product("FILE_SHARING");
        feature = new Feature(product, "ADD_FILE");
        feature.getLimits().add(usageLimit);

        usageLimitVerifier = new UsageLimitVerifierImpl(usageLimitResolver, limitVerificationStrategyResolver, usageRepo);
    }

    @Test
    void recordUsageShouldCallLimitVerificationStrategy() {
        initMocks();

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = mockStatic(ZonedDateTime.class)) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime);

            usageLimitVerifier.recordFeatureUsage(feature, userGrouping, Collections.singletonMap("MAX_FILES", 2L));
        }

        verify(limitVerificationStrategy).recordFeatureUsage(limitTrackingContext, usageLimit,2L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void recordUsageWhenAdditionalUnitsEmptyShouldThrowException(Map<String, Long> additionalUnits) {

        assertThrows(IllegalArgumentException.class, () -> usageLimitVerifier.recordFeatureUsage(feature, userGrouping, additionalUnits));

    }

    @Test
    void recordUsageWhenAdditionalUnitsNegativeShouldThrowException() {

        var additionalUnits = Collections.singletonMap("MAX_FILES", -1L);
        assertThrows(IllegalArgumentException.class, () -> usageLimitVerifier.recordFeatureUsage(feature, userGrouping, additionalUnits));

    }

    @ParameterizedTest
    @NullAndEmptySource
    void reduceUsageWhenReducedUnitsEmptyShouldThrowException(Map<String, Long> additionalUnits) {

        assertThrows(IllegalArgumentException.class, () -> usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, additionalUnits));

    }

    @Test
    void reduceUsageWhenReducedlUnitsNegativeShouldThrowException() {

        var additionalUnits = Collections.singletonMap("MAX_FILES", -1L);
        assertThrows(IllegalArgumentException.class, () -> usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, additionalUnits));

    }

    // TODO : Complete other cases when data not found in repos : Limits, LimitsVerificationStrategy, etc.


    private void initMocks() {
        when(usageLimitResolver.resolveUsageLimit(feature, "MAX_FILES", userGrouping))
                .thenReturn(Optional.of(usageLimit));

        when(limitVerificationStrategyResolver.resolveLimitVerificationStrategy(usageLimit))
                .thenReturn(limitVerificationStrategy);

        String instantExpected = "2022-03-14T09:33:52Z";
        zonedDateTime = ZonedDateTime.parse(instantExpected);

        when(limitVerificationStrategy.getWindowStart(usageLimit, zonedDateTime)).thenReturn(Optional.empty());

        RecordSearchCriteria searchCriteria = new RecordSearchCriteria("MAX_FILES", Optional.empty(), Optional.empty());
        when(usageRepo.loadUsageData(feature, userGrouping, Collections.singletonList(searchCriteria))).thenReturn(limitTrackingContext);
    }

}