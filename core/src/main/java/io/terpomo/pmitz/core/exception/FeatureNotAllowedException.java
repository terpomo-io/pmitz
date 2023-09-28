package io.terpomo.pmitz.core.exception;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public class FeatureNotAllowedException extends RuntimeException{
    private final Feature feature;
    private final UserGrouping userGrouping;

    public FeatureNotAllowedException(String message, Feature feature, UserGrouping userGrouping) {
        super(message);
        this.feature = feature;
        this.userGrouping = userGrouping;
    }
}
