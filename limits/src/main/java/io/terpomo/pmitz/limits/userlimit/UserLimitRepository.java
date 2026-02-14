/*
 * Copyright 2023-2026 the original author or authors.
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

package io.terpomo.pmitz.limits.userlimit;

import java.util.Optional;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;

public interface UserLimitRepository {

	Optional<LimitRule> findLimitRule(FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping);

	void updateLimitRule(FeatureRef featureRef, LimitRule limitRule, UserGrouping userGrouping);

	void deleteLimitRule(FeatureRef featureRef, String limitRuleId, UserGrouping userGrouping);

	static UserLimitRepository.Builder builder() {
		return new DefaultUserLimitRepositoryBuilder();
	}

	interface Builder {
		UserLimitRepository jdbcRepository(DataSource dataSource, String dbSchema, String tableName);

		UserLimitRepository noOpRepository();
	}
}
