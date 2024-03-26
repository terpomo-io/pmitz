package io.terpomo.pmitz.core.limits.types;

import java.time.ZonedDateTime;
import java.util.Optional;

import io.terpomo.pmitz.core.limits.UsageLimit;

public class CountLimit extends UsageLimit {

	private long count;

	public CountLimit(String id, long count) {
		super(id);
		this.count = count;
	}

	@Override
	public long getValue() {
		return count;
	}

	@Override
	public Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate) {
		return Optional.empty();
	}

	@Override
	public Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate) {
		return Optional.empty();
	}
}
