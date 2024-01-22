package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.*;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UsageLimitVerifierImpl implements UsageLimitVerifier {
    private final UsageLimitResolver usageLimitResolver;

    private final UsageLimitVerificationStrategyResolver limitVerifierStrategyResolver;
    private final UsageRepository usageRepository;

    public UsageLimitVerifierImpl(UsageLimitResolver usageLimitResolver,
                                  UsageLimitVerificationStrategyResolver limitVerifierStrategyResolver,
                                  UsageRepository usageRepository) {
        this.usageLimitResolver = usageLimitResolver;
        this.usageRepository = usageRepository;
        this.limitVerifierStrategyResolver = limitVerifierStrategyResolver;
    }

    @Override
    public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        if (additionalUnits == null || additionalUnits.isEmpty()){
            throw new IllegalArgumentException("Additional units cannot be empty");
        }
        if (additionalUnits.values().stream().anyMatch(v -> v == null || v <=0)){
            throw new IllegalArgumentException("Additional units must be positive numbers");
        }
        recordOrReduce(feature, userGrouping, additionalUnits, true);

    }

    @Override
    public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
        if (reducedUnits == null || reducedUnits.isEmpty()){
            throw new IllegalArgumentException("Reduced units cannot be empty");
        }
        if (reducedUnits.values().stream().anyMatch(v -> v == null || v <=0)){
            throw new IllegalArgumentException("Reduced units must be positive numbers");
        }
        recordOrReduce(feature, userGrouping, reducedUnits, false);
    }

    @Override
    public Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping) {
        var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

        var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

        var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);

        usageRepository.loadUsageData(context);

        return limitVerificationStrategiesMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getId(), entry -> entry.getValue().getRemainingUnits(context, entry.getKey())));
    }

    @Override
    public boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

        var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

        var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);
        usageRepository.loadUsageData(context);

        return limitVerificationStrategiesMap.entrySet().stream()
                .allMatch(entry -> entry.getValue().isWithinLimits(context, entry.getKey(), additionalUnits.get(entry.getKey().getId())));

    }

    private void recordOrReduce (Feature feature, UserGrouping userGrouping, Map<String, Long> units, boolean isRecord){

        var limitVerificationStrategiesMap = findVerificationStrategiesByLimit(feature, userGrouping);

        var limitSearchCriteriaList = gatherSearchCriteria(limitVerificationStrategiesMap);

        var context = new LimitTrackingContext(feature, userGrouping, limitSearchCriteriaList);

        if (isRecord) {
            limitVerificationStrategiesMap
                    .forEach((usageLimit, verifStrategy) -> verifStrategy.recordFeatureUsage(context, usageLimit, units.get(usageLimit.getId())));
        } else {
            limitVerificationStrategiesMap
                    .forEach((usageLimit, verifStrategy) -> verifStrategy.reduceFeatureUsage(context, usageLimit, units.get(usageLimit.getId())));
        }

        usageRepository.updateUsageRecords(context);
    }

    private List<RecordSearchCriteria> gatherSearchCriteria (Map<UsageLimit, UsageLimitVerificationStrategy> verificationStrategyMap){
        return verificationStrategyMap.entrySet().stream()
                .map(entry -> getLimitSearchCriteria(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }
    private Map<UsageLimit, UsageLimitVerificationStrategy> findVerificationStrategiesByLimit (Feature feature, UserGrouping userGrouping){
        return feature.getLimitsIds().stream()
                .map(limitId -> usageLimitResolver.resolveUsageLimit(feature, limitId, userGrouping))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Function.identity(), limitVerifierStrategyResolver::resolveLimitVerificationStrategy));
    }

    private RecordSearchCriteria getLimitSearchCriteria (UsageLimitVerificationStrategy strategy, UsageLimit limit){
        Optional<ZonedDateTime> windowStart = strategy.getWindowStart(limit, ZonedDateTime.now());
        Optional<ZonedDateTime> windowEnd = strategy.getWindowEnd(limit, ZonedDateTime.now());
        return new RecordSearchCriteria(limit.getId(), windowStart.orElse(null),
                windowEnd.orElse(null));
    }
}
