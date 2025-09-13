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

package io.terpomo.pmitz;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.FeatureNotFoundException;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.LimitVerifier;
import io.terpomo.pmitz.remote.client.LimitVerifierRemoteClient;
import io.terpomo.pmitz.remote.client.RemoteCallException;

public class RemoteClientSample {

	private static Random random = new SecureRandom();

	public static void main(String[] args) {

		String remoteServerUrl = "http://localhost:8080";

		System.out.printf("URL for remote server is %s. Plase make sure server is running%n", remoteServerUrl);

		RemoteClientSample remoteClientSample = new RemoteClientSample();

		var limitVerifier = remoteClientSample.initRemoteLimitVerifier(remoteServerUrl, "/product-library.json");

		var product = new Product("library");
		var feature = new Feature(product, "reserve");
		try {
			UserGrouping user = new IndividualUser("user00" + random.nextInt(10_000));
			var withinLimits = limitVerifier.isWithinLimits(feature, user, Map.of("maxborrowed", 7L));

			System.out.println("Can user borrow 7 books? " + withinLimits);

			withinLimits = limitVerifier.isWithinLimits(feature, user, Map.of("maxborrowed", 5L));

			System.out.println("Can user borrow 5 books? " + withinLimits);

			limitVerifier.recordFeatureUsage(feature, user, Map.of("maxborrowed", 5L));
			System.out.println("Books reserved!");

			Map<String, Long> remainingUnits = limitVerifier.getLimitsRemainingUnits(feature, user);
			System.out.println("Remaining units " + remainingUnits);
		}
		catch (FeatureNotFoundException ex) {
			System.out.println("Invalid feature or product");
		}
		catch (LimitExceededException ex) {
			System.out.println("Feature not allowed at this time because the limit is exceeded.");
		}
		catch (RemoteCallException ex) {
			System.out.println("Unexpected error encountered during remote call " + ex.getMessage());
		}
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

