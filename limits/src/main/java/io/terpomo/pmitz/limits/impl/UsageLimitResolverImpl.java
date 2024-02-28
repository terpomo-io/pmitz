/*
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

package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.repository.userlimit.UserLimitRepository;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitResolver;

import java.util.Optional;

public class UsageLimitResolverImpl implements UsageLimitResolver {

    private ProductRepository productRepository;
    private UserLimitRepository userLimitRepository;

    @Override
    public Optional<UsageLimit> resolveUsageLimit(Feature feature, String usageLimitId, UserGrouping userGrouping) {
        //TODO verify interfaces' specs
        // 1- find limit specific to userGrouping
        // 2- find limit for plan --> 2.1 find plan, 2.2 find limit for plan
        // 3- find global limit

        return userLimitRepository.findUsageLimit(feature, usageLimitId, userGrouping)
                .or(() -> productRepository.getGlobalLimit(feature, usageLimitId));

    }
}
