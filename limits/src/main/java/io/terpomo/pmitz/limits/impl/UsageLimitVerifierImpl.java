package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.*;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UsageLimitVerifierImpl implements UsageLimitVerifier {
    private UsageLimitResolver usageLimitResolver;

    private LimitVerificationStrategyResolver limitVerifierStrategyResolver;
    private UsageRepository usageRepository;

    public UsageLimitVerifierImpl(UsageLimitResolver usageLimitResolver, UsageRepository usageRepository) {
        this.usageLimitResolver = usageLimitResolver;
        this.usageRepository = usageRepository;
    }

    @Override
    public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        recordOrReduce(feature, userGrouping, additionalUnits, true);

    }

    @Override
    public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
        recordOrReduce(feature, userGrouping, reducedUnits, true);
    }

    @Override
    public Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
        return null;
    }

    private void recordOrReduce (Feature feature, UserGrouping userGrouping, Map<String, Long> units, boolean isRecord){

        var limitVerificationStrategiesMap = feature.getLimitsIds().stream()
                .map(limitId -> usageLimitResolver.resolveUsageLimit(feature, limitId, userGrouping))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Function.identity(), limitVerifierStrategyResolver::resolveLimitVerificationStrategy));

        var limitSearchCriteriaList = limitVerificationStrategiesMap.entrySet().stream()
                .map(entry -> getLimitSearchCriteria(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        var context = usageRepository.loadUsageData(feature, userGrouping, limitSearchCriteriaList);

        if (isRecord) {
            limitVerificationStrategiesMap.entrySet().stream()
                    .forEach(entry -> entry.getValue().recordFeatureUsage(context, entry.getKey(), units.get(entry.getKey().getId())));
        } else {
            limitVerificationStrategiesMap.entrySet().stream()
                    .forEach(entry -> entry.getValue().reduceFeatureUsage(context, entry.getKey(), units.get(entry.getKey().getId())));
        }
    }


    @Override
    public boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        var limitVerificationStrategiesMap = feature.getLimitsIds().stream()
                .map(limitId -> usageLimitResolver.resolveUsageLimit(feature, limitId, userGrouping))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Function.identity(), limitVerifierStrategyResolver::resolveLimitVerificationStrategy));

        var limitSearchCriteriaList = limitVerificationStrategiesMap.entrySet().stream()
                .map(entry -> getLimitSearchCriteria(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        var context = usageRepository.loadUsageData(feature, userGrouping, limitSearchCriteriaList);

        return limitVerificationStrategiesMap.entrySet().stream()
                .allMatch(entry -> entry.getValue().isWithinLimits(context, entry.getKey(), additionalUnits.get(entry.getKey().getId())));

    }

    private RecordSearchCriteria getLimitSearchCriteria (LimitVerificationStrategy strategy, UsageLimit limit){
        return new RecordSearchCriteria(limit.getId(), strategy.getWindowStart(limit, ZonedDateTime.now()),
                strategy.getWindowEnd(limit, ZonedDateTime.now()));
    }

}
