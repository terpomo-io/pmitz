package io.terpomo.pmitz.core.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Optional;

public interface UserLimitRepository {

    Optional<UsageLimit> findUsageLimit (Feature feature, String usageLimitId, UserGrouping userGrouping);
}
