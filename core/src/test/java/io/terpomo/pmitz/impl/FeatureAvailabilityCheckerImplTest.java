package io.terpomo.pmitz.impl;

import io.terpomo.pmitz.core.Feature;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FeatureAvailabilityCheckerImplTest {

    private FeatureAvailabilityCheckerImpl featureAvailabilityChecker;

    @Mock
    private UsageLimitVerifier usageLimitVerifier;

    @Mock
    private SubscriptionVerifier subscriptionVerifier;

    @BeforeEach
    void init(){
        featureAvailabilityChecker = new FeatureAvailabilityCheckerImpl(usageLimitVerifier, subscriptionVerifier);
    }
    @Test
    void givenFeatureAllowedAndLimitNotExceededWhenIsFeatureAvailableThenReturnTrue(){

        String limitId = "file uploads";
        Feature feature = new Feature();
        feature.setLimitsIds(Collections.singletonList(limitId));
        UserGrouping userGrouping = new IndividualUser("user001");

        when(subscriptionVerifier.isFeatureAllowed(feature, userGrouping)).thenReturn(true);
        when(usageLimitVerifier.isLimitExceeded(feature, limitId, userGrouping)).thenReturn(false);

        assertTrue (featureAvailabilityChecker.isFeatureAvailable(feature, userGrouping));
    }

    @Test
    void givenFeatureAllowedAndLimitExceededWhenIsFeatureAvailableThenReturnFalse(){

    }

    void givenFeatureNotAllowedAndLimitNotExceededWhenIsFeatureAvailableThenReturnFalse(){

    }

    void givenFeatureNotAllowedAndLimitExceededWhenIsFeatureAvailableThenReturnTrue(){

    }

    void givenNoSubscriptionAndLimitNotExceededWhenIsFeatureAvailableThenReturnTrue(){

    }

    void givenNoSubscriptionAndLimitExceededWhenIsFeatureAvailableThenReturnFalse(){

    }

    void givenFeatureAllowedAndNoLimitWhenIsFeatureAvailableThenReturnTrue(){

    }

    void givenFeatureNotAllowedAndNoLimitWhenIsFeatureAvailableThenReturnFalse(){

    }

    void givenFeatureAllowedAndLimitNotExceedWhenGetFeatureStatusThenReturnTrue(){

    }

    void givenFeatureAllowedAndLimitExceededWhenGetFeatureStatusThenReturnFalse(){

    }

    void givenFeatureNotAllowedAndLimitNotExceededWhenGetFeatureStatusThenReturnFalse(){

    }

    void givenFeatureNotAllowedAndLimitExceededWhenGetFeatureStatusThenReturnTrue(){

    }

    void givenNoSubscriptionAndLimitNotExceededWhenGetFeatureStatusThenReturnTrue(){

    }

    void givenNoSubscriptionAndLimitExceededWhenGetFeatureStatusThenReturnFalse(){

    }

    void givenFeatureAllowedAndNoLimitWhenGetFeatureStatusThenReturnTrue(){

    }

    void givenFeatureNotAllowedAndNoLimitWhenGetFeatureStatusThenReturnFalse(){

    }

}
