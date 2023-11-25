package io.terpomo.pmitz.core.repository.product.inmemory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = CountLimit.class, name = "CountLimit"),
	@JsonSubTypes.Type(value = RateLimit.class, name = "RateLimit")
})
@ExcludeFromJacocoGeneratedReport
public class UsageLimitMixIn {
}
