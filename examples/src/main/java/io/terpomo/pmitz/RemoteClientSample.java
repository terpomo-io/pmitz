/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.Map;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.FeatureNotFoundException;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.remote.client.RemoteCallException;
import io.terpomo.pmitz.remote.client.UsageLimitVerifierRemoteClient;

public class RemoteClientSample {

	public static void main(String[] args) {
		RemoteClientSample remoteClientSample = new RemoteClientSample();

		var usageLimitVerifier = remoteClientSample.initRemoteLimitVerifier("http://localhost:8080", "/product-library.json");

		var product = new Product("library");
		var feature = new Feature(product, "reserve");
		try {
			usageLimitVerifier.recordFeatureUsage(feature, new IndividualUser("user001"), Map.of("maxborrowed", 5L));
			System.out.println("Books reserved!");
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

	private UsageLimitVerifier initRemoteLimitVerifier(String remoteServerUrl, String productClasspath) {
		var usageLimitVerifier = new UsageLimitVerifierRemoteClient(remoteServerUrl);

		var inputStream = RemoteClientSample.class.getResourceAsStream(productClasspath);

		try {
			usageLimitVerifier.uploadProduct(inputStream);
			System.out.println("Product uploaded.");
		}
		catch (RepositoryException repositoryException) {
			System.out.println("Error encountered while uploading product and feature info : " + repositoryException.getMessage());
		}

		return usageLimitVerifier;
	}
}

