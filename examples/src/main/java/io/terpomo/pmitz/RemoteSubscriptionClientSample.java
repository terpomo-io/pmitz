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

package io.terpomo.pmitz;

import java.security.SecureRandom;
import java.util.Map;

import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.limits.LimitVerifier;
import io.terpomo.pmitz.remote.client.LimitVerifierRemoteClient;
import io.terpomo.pmitz.remote.client.SubscriptionRepoRemoteClient;
import io.terpomo.pmitz.remote.client.SubscriptionVerifierRemoteClient;

public class RemoteSubscriptionClientSample {

	private static int subscriptionNumber = new SecureRandom().nextInt(1000, 10_000);

	public static void main(String[] args) {
		String remoteServerUrl = "http://localhost:8080";

		System.out.printf("URL for remote server is %s. Plase make sure server is running%n", remoteServerUrl);

		RemoteSubscriptionClientSample remoteClientSample = new RemoteSubscriptionClientSample();

		try {
			remoteClientSample.initRemoteLimitVerifier(remoteServerUrl, "/product-library.json");
		}
		catch (RepositoryException ex) {
			System.out.printf("An error has occurred: %s%n", ex.getMessage());
		}

		var subscriptionRepo = new SubscriptionRepoRemoteClient(remoteServerUrl);

		Subscription subscription = new Subscription("sub-terpomo-" + subscriptionNumber);
		subscription.setPlans(Map.of("library", "basic"));
		subscription.setStatus(SubscriptionStatus.ACTIVE);

		try {
			subscriptionRepo.create(subscription);
		}
		catch (RepositoryException ex) {
			System.out.printf("An error has occurred: %s%n", ex.getMessage());
		}

		var subscriptionVerifier = new SubscriptionVerifierRemoteClient(remoteServerUrl);

		boolean featureAllowed = subscriptionVerifier.isFeatureAllowed(new FeatureRef("library", "reserve"), subscription);

		System.out.println("Subscription verifier feature allowed is :" + featureAllowed);

	}

	private LimitVerifier initRemoteLimitVerifier(String remoteServerUrl, String productClasspath) {

		var limitVerifier = new LimitVerifierRemoteClient(remoteServerUrl);

		var inputStream = RemoteClientSample.class.getResourceAsStream(productClasspath);

		try {
			limitVerifier.uploadProduct(inputStream);
			System.out.println("Product uploaded.");
		}
		catch (RepositoryException repositoryException) {
			System.out.println("Error encountered while uploading product and feature info : " + repositoryException.getMessage());
		}

		return limitVerifier;
	}
}
