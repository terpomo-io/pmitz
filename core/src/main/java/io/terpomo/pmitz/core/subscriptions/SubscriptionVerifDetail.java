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

package io.terpomo.pmitz.core.subscriptions;

import java.util.Optional;

public final class SubscriptionVerifDetail {

	private boolean featureAllowed;
	private ErrorCause errorCause;
	private Subscription fetchedSubscription;

	@SuppressWarnings("unused")
	private SubscriptionVerifDetail() {
		// Default constructor for Jackson deserialization
	}

	private SubscriptionVerifDetail(boolean featureAllowed, ErrorCause validationErrorCause) {
		this.featureAllowed = featureAllowed;
		this.errorCause = validationErrorCause;
	}

	public static SubscriptionVerifDetail verificationOk() {
		return new SubscriptionVerifDetail(true, null);
	}

	public static SubscriptionVerifDetail verificationError(ErrorCause errorCause) {
		return new SubscriptionVerifDetail(false, errorCause);
	}

	public SubscriptionVerifDetail withFetchedSubscription(Subscription fetchedSubscription) {
		this.fetchedSubscription = fetchedSubscription;
		return this;
	}

	public boolean isFeatureAllowed() {
		return featureAllowed;
	}

	public ErrorCause getErrorCause() {
		return errorCause;
	}

	public Optional<Subscription> getFetchedSubscription() {
		return Optional.ofNullable(fetchedSubscription);
	}

	public enum ErrorCause {
		INVALID_SUBSCRIPTION,
		PRODUCT_NOT_ALLOWED,
		FEATURE_NOT_ALLOWED
	}
}

