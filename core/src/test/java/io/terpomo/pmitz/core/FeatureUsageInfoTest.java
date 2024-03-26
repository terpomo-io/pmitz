package io.terpomo.pmitz.core;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureUsageInfoTest {

	@Test
	void isLimitReachedShouldReturnTrueWhenAnyRemainingUnitIsZero() {
		Map<String, Long> remainingUnits = new HashMap<>();
		remainingUnits.put("UPLOAD_FILES", 1L);
		remainingUnits.put("MONTHLY_DATA", 0L);

		var info = new FeatureUsageInfo(FeatureStatus.AVAILABLE, remainingUnits);

		assertTrue(info.isLimitReached());
	}

	@Test
	void isLimitReachedShouldReturnFalseWhenNoRemainingUnitIsZero() {
		Map<String, Long> remainingUnits = new HashMap<>();
		remainingUnits.put("UPLOAD_FILES", 1L);
		remainingUnits.put("MONTHLY_DATA", 2L);

		var info = new FeatureUsageInfo(FeatureStatus.AVAILABLE, remainingUnits);

		assertFalse(info.isLimitReached());
	}
}