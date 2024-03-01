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

package io.terpomo.pmitz.core.repository.userlimit;

import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface UserLimitRepository {

	Optional<UsageLimit> findUsageLimit(Feature feature, String usageLimitId, UserGrouping userGrouping);

	void addUsageLimit(Feature feature, UsageLimit usageLimit, UserGrouping userGrouping);

	void updateUsageLimit(Feature feature, UsageLimit usageLimit, UserGrouping userGrouping);

	void deleteUsageLimit(Feature feature, String usageLimitId, UserGrouping userGrouping);
}
