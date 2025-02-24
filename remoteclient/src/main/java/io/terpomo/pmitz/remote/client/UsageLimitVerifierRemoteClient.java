/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.remote.client;

import java.io.InputStream;
import java.util.Map;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.remote.client.http.PmitzHttpClient;

public class UsageLimitVerifierRemoteClient implements UsageLimitVerifier {

	private PmitzClient pmitzClient;

	public UsageLimitVerifierRemoteClient(String url) {
		pmitzClient = new PmitzHttpClient(url);
	}

	@Override
	public Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
		var usageInfo = pmitzClient.getUsageInfo(feature, userGrouping);
		return usageInfo.remainingUsageUnits();
	}

	@Override
	public boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {

		//TODO complete implementation
		return false;
	}

	@Override
	public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
		pmitzClient.recordOrReduce(feature, userGrouping, additionalUnits, false);

	}

	@Override
	public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
		pmitzClient.recordOrReduce(feature, userGrouping, reducedUnits, true);
	}

	public void uploadProduct(InputStream inputStream) {
		((PmitzHttpClient) pmitzClient).uploadProduct(inputStream);
	}

}
