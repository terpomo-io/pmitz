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

package io.terpomo.pmitz.core.subjects;

import java.io.Serializable;
import java.util.Optional;

public abstract class UserGrouping implements Serializable {

	public abstract String getId();


	/**
	 * This method indicated whether the user grouping is allowed to access the current product.
	 *
	 * Implementations of this abstract class might override this method to add actual logic,
	 * such as in @{@link io.terpomo.pmitz.core.subscriptions.Subscription}
	 * @param productId <code>String</code> the product Id
	 * @return <code>boolean</code> <code>true</code> if the user grouping is allowed to access the current
	 * product, <code>false</code> otherwise.
	 */
	public boolean isProductAllowed(String productId) {
		return true;
	}

	/**
	 * This method returns the id of the plan (if any) associated with the user grouping, for the requested product.
	 *
	 * Implementations of this abstract class might override this method to add actual logic,
	 * such as in @{@link io.terpomo.pmitz.core.subscriptions.Subscription}
	 * @param productId <code>String</code> the product Id
	 * @return <code>Optional</code> containing the id of the plan. If no plan is associated with the user grouping,
	 * this method returns an empty <code>Optional</code>.
	 */
	public Optional<String> getPlan(String productId) {
		return Optional.empty();
	}

}
