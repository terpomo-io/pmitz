package io.terpomo.pmitz.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureUsageTrackerImplTest {

    @Mock
    UsageLimitVerifier limitVerifier;
    @Mock
    SubscriptionVerifier subscriptionVerifier;

    Feature feature;

    UserGrouping userGrouping = new IndividualUser("user001");

    FeatureUsageTrackerImpl featureUsageTracker;

    @BeforeEach
    void init (){
        feature = new Feature(null, "FILE_UPLOAD");

        featureUsageTracker = new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
    }

    @Test
    void givenFeatureNotAllowedWhenRecordFeatureUsageThenThrowFeatureNotAllowedException(){
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

        assertThrows(FeatureNotAllowedException.class,
                () -> featureUsageTracker.recordFeatureUsage(feature, userGrouping, Collections.singletonMap("FILE_SIZE", 1000L)));

    }

    @Test
    void givenFeatureAllowedWhenRecordFeatureUsageThenCallLimitVerifier() {
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);

        var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
        featureUsageTracker.recordFeatureUsage(feature, userGrouping, additionalUnits);

        verify(limitVerifier).recordFeatureUsage(feature, userGrouping, additionalUnits);
    }

    @Test
    void givenFeatureWhenReduceFeatureUsageThenCallLimitVerifier() {
        var reducedUnits = Collections.singletonMap("UPLOADED_FILES", 1L);
        featureUsageTracker.reduceFeatureUsage(feature, userGrouping, reducedUnits);

        verify(limitVerifier).reduceFeatureUsage(feature, userGrouping, reducedUnits);
    }

    @Test
    void givenFeatureNotAllowedWhenVerifyLimitsThenFeatureStatusNotAllowed() {
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

        var additionalUnits = Collections.singletonMap("FILE_SIZE", 1000L);
        var featureInfo = featureUsageTracker.verifyLimits(feature, userGrouping, additionalUnits);

        assertEquals(FeatureStatus.NOT_ALLOWED, featureInfo.featureStatus());
        assertTrue(featureInfo.remainingUsageUnits().isEmpty());
    }

    @Test
    void givenAnyLimitExceededWhenVerifyLimitsThenFeatureStatusNotAllowed() {
        String limitId = "FILE_UPLOAD";
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);
        when(limitVerifier.getLimitsRemainingUnits(feature, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

        var additionalUnits = Collections.singletonMap(limitId, 2L);
        var featureInfo = featureUsageTracker.verifyLimits(feature, userGrouping, additionalUnits);

        assertEquals(FeatureStatus.LIMIT_EXCEEDED, featureInfo.featureStatus());
        assertEquals(-1, featureInfo.remainingUsageUnits().get(limitId));
    }

    @Test
    void givenFeatureNotAllowedWhenGetUsageInfoThenReturnUsageInfo() {
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(false);

        FeatureUsageInfo featureUsageInfo = featureUsageTracker.getUsageInfo(feature, userGrouping);
        assertEquals(FeatureStatus.NOT_ALLOWED, featureUsageInfo.featureStatus());
        assertTrue(featureUsageInfo.remainingUsageUnits().isEmpty());
    }

    @Test
    void givenFeatureAllowedWhenGetUsageInfoThenReturnRemainingUnits() {
        String limitId = "ADD_USER";
        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);
        when(limitVerifier.getLimitsRemainingUnits(feature, userGrouping)).thenReturn(Collections.singletonMap(limitId, 1L));

        var featureInfo = featureUsageTracker.getUsageInfo(feature, userGrouping);

        assertEquals(FeatureStatus.AVAILABLE, featureInfo.featureStatus());
        assertEquals(1L, featureInfo.remainingUsageUnits().get(limitId));

        //TODO question : What if there is no remaining units available. Should status be still Available?
        // shall we introduce a new status : LIMIT_REACHED, that is : limit is not exceeded but no more room for additional units
        //

    }

}