package io.terpomo.pmitz.limits.impl;

import java.util.Map;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.limits.impl.remote.PmitzClient;

public class UsageLimitVerifierRemoteClient implements UsageLimitVerifier {

	private PmitzClient pmitzClient;
	@Override
	public boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {

		return false;
	}

	@Override
	public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {

	}

	@Override
	public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {

	}

	@Override
	public Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
		return null;
	}
}
