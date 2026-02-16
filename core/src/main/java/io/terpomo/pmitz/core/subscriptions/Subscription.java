/*
 * Copyright 2023-2025 the original author or authors.
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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.terpomo.pmitz.core.subjects.UserGrouping;

public class Subscription extends UserGrouping {

	private String subscriptionId;
	private SubscriptionStatus status;
	private ZonedDateTime expirationDate;

	private Map<String, String> plansByProduct;

	@SuppressWarnings("unused")
	private Subscription() {
		// Default constructor for Jackson deserialization
	}

	public Subscription(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public boolean isActive() {
		return SubscriptionStatus.ACTIVE == status;
	}

	public boolean isExpired() {
		return status == null || !status.isValid() || (status.isValid() && isExpirationDatePassed());
	}

	public boolean isValid() {
		return status != null && status.isValid() && !isExpirationDatePassed();
	}

	@Override
	public String getId() {
		return subscriptionId;
	}

	public String getSubscriptionId() {
		return getId();
	}

	public SubscriptionStatus getStatus() {
		return status;
	}

	public void setStatus(SubscriptionStatus status) {
		this.status = status;
	}

	public ZonedDateTime getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(ZonedDateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	public Map<String, String> getPlansByProduct() {
		return plansByProduct;
	}

	@Override
	public boolean isProductAllowed(String productId) {
		return plansByProduct != null && plansByProduct.containsKey(productId);
	}

	@Override
	public Optional<String> getPlan(String productId) {
		return (plansByProduct != null) ? Optional.ofNullable(plansByProduct.get(productId)) : Optional.empty();
	}

	public void setPlans(Map<String, String> plansByProduct) {
		this.plansByProduct = plansByProduct;
	}

	private boolean isExpirationDatePassed() {
		return expirationDate != null && expirationDate.isBefore(ZonedDateTime.now());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Subscription that = (Subscription) o;
		return Objects.equals(subscriptionId, that.subscriptionId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(subscriptionId);
	}
}
