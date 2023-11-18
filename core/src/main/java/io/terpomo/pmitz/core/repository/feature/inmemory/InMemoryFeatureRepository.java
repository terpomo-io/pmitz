/*
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

package io.terpomo.pmitz.core.repository.feature.inmemory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.repository.feature.FeatureRepository;

public class InMemoryFeatureRepository implements FeatureRepository {

	private Map<String, Map<String, Feature>> features = new HashMap<>();

	private ObjectMapper mapper;

	public InMemoryFeatureRepository() {

		mapper = new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.addMixIn(Feature.class, FeatureMixIn.class)
				.addMixIn(UsageLimit.class, UsageLimitMixIn.class)
				.addMixIn(RateLimit.class, RateLimitMixIn.class)
				.addMixIn(CountLimit.class, CountLimitMixIn.class);
	}

	@Override
	public List<Feature> getFeatures(Product product) {

		validateProduct(product);

		Map<String, Feature> featuresMap = features.get(product.getProductId());

		return featuresMap == null ?
				List.of() :	featuresMap.values().stream().toList();
	}

	@Override
	public Optional<Feature> getFeature(Product product, String featureId) {

		validateProduct(product);
		validateFeatureId(featureId);

		Map<String, Feature> featuresMap = features.get(product.getProductId());

		return featuresMap == null ?
				Optional.empty() : Optional.ofNullable(featuresMap.get(featureId));
	}

	@Override
	public Optional<UsageLimit> getGlobalLimit(Feature feature, String usageLimitId) {

		validateFeature(feature);

		if (feature.getLimits() == null) {
			return Optional.empty();
		}

		return feature.getLimits().stream()
				.filter(l -> l.getId().equals(usageLimitId))
				.findFirst();
	}

	@Override
	public void addFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();
		Map<String, Feature> featuresMap = features.get(productId);
		if (featuresMap == null) {
			featuresMap = new HashMap<>();
			features.put(productId, featuresMap);
		} else {
			if (featuresMap.containsKey(feature.getFeatureId())) {
				throw new RepositoryException(String.format("Feature '%s' already exist", feature.getFeatureId()));
			}
		}

		featuresMap.put(feature.getFeatureId(), feature);
	}

	@Override
	public void updateFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();
		Map<String, Feature> featuresMap = features.get(productId);
		if (featuresMap == null || !featuresMap.containsKey(feature.getFeatureId())) {
			throw new RepositoryException(String.format("Feature '%s' not found for product '%s'", feature.getFeatureId(), productId));
		}

		featuresMap.put(feature.getFeatureId(), feature);
	}

	@Override
	public void removeFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();
		Map<String, Feature> featuresMap = features.get(productId);
		if (featuresMap == null || !featuresMap.containsKey(feature.getFeatureId())) {
			throw new RepositoryException(String.format("Feature '%s' not found for product '%s'", feature.getFeatureId(), productId));
		}

		featuresMap.remove(feature.getFeatureId());
	}

	@Override
	public void clear() {
		features.clear();
	}

	@Override
	public void load(InputStream inputStream) throws IOException {
		TypeReference<List<Feature>> typeRef = new TypeReference<>() {};
		List<Feature> featuresReaded = mapper.readValue(inputStream, typeRef);

		features = featuresReaded.stream()
				.collect(Collectors.groupingBy(
						feature -> feature.getProduct().getProductId(),
						Collectors.toMap(Feature::getFeatureId, Function.identity(), (existing, replacement) -> existing, HashMap::new)
				));
	}

	@Override
	public void store(OutputStream outputStream) throws IOException {

		List<Feature> featuresList = features.values().stream()
				.flatMap(map -> map.values().stream())
				.collect(Collectors.toList());

		mapper.writeValue(outputStream, featuresList);
	}

	private void validateFeature(Feature feature) {

		if (feature == null) {
			throw new RepositoryException(("Feature must not be 'null'"));
		}
		validateFeatureId(feature.getFeatureId());
		validateProduct(feature.getProduct());
	}
	private void validateFeatureId(String featureId) {

		if (featureId == null) {
			throw new RepositoryException(("FeatureId must not be 'null'"));
		}
	}
	private void validateProduct(Product product) {

		if (product == null || product.getProductId() == null) {
			throw new RepositoryException(("Product or productId must not be 'null'"));
		}
	}
}
