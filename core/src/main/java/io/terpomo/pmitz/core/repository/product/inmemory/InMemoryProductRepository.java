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

package io.terpomo.pmitz.core.repository.product.inmemory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.core.repository.product.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {

	public static final String PRODUCT_NOT_FOUND = "Product '%s' not found";
	private final ObjectMapper mapper;
	private Map<String, Product> products = new HashMap<>();

	public InMemoryProductRepository() {

		this.mapper = new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.addMixIn(Product.class, ProductMixIn.class)
				.addMixIn(Feature.class, FeatureMixIn.class)
				.addMixIn(UsageLimit.class, UsageLimitMixIn.class)
				.addMixIn(SlidingWindowRateLimit.class, SlidingWindowRateLimitMixIn.class)
				.addMixIn(CalendarPeriodRateLimit.class, CalendarPeriodRateLimitMixIn.class)
				.addMixIn(CountLimit.class, CountLimitMixIn.class);
	}

	@Override
	public List<String> getProductIds() {

		return this.products.values().stream()
				.map(Product::getProductId)
				.toList();
	}

	@Override
	public Optional<Product> getProductById(String productId) {

		validateProductId(productId);

		return Optional.ofNullable(this.products.get(productId));
	}

	@Override
	public void addProduct(Product product) {

		validateProduct(product);

		this.products.compute(product.getProductId(), (productId, existingProduct) -> {
			if (existingProduct != null) {
				throw new RepositoryException(String.format("Product '%s' already exists", productId));
			}
			linkProductFeatures(product);
			return product;
		});
	}

	@Override
	public void removeProduct(Product product) {

		validateProduct(product);

		this.getProductById(product.getProductId())
				.ifPresentOrElse(
						existingProduct -> this.products.remove(product.getProductId()),
						() -> {
							throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, product.getProductId()));
						}
				);
	}

	@Override
	public List<Feature> getFeatures(Product product) {

		validateProduct(product);

		return this.getProductById(product.getProductId())
				.map(Product::getFeatures)
				.orElseThrow(() -> new RepositoryException(String.format(PRODUCT_NOT_FOUND, product.getProductId())));
	}

	@Override
	public Optional<Feature> getFeature(Product product, String featureId) {

		validateProduct(product);
		validateFeatureId(featureId);

		return this.getProductById(product.getProductId())
				.flatMap(p -> p.getFeatures().stream()
						.filter(f -> f.getFeatureId().equals(featureId))
						.findFirst());
	}

	@Override
	public Optional<UsageLimit> getGlobalLimit(Feature feature, String usageLimitId) {

		validateFeature(feature);

		return Optional.ofNullable(feature.getProduct())
				.map(Product::getProductId)
				.flatMap(productId -> Optional.ofNullable(this.products.get(productId)))
				.flatMap(p -> p.getFeatures().stream()
						.filter(feature::equals)
						.findFirst())
				.flatMap(f -> Optional.ofNullable(f.getLimits())
						.flatMap(ll -> ll.stream()
								.filter(l -> l.getId().equals(usageLimitId))
								.findFirst()));
	}

	@Override
	public void addFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();
		Product existingProduct = this.products.get(productId);

		if (existingProduct == null) {
			throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
		}

		if (existingProduct.getFeatures().contains(feature)) {
			throw new RepositoryException(String.format("Feature '%s' already exists", feature.getFeatureId()));
		}

		existingProduct.getFeatures().add(feature);
	}

	@Override
	public void updateFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();

		this.products.compute(productId, (key, existingProduct) -> {

			if (existingProduct == null) {
				throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
			}

			OptionalInt indexOpt = IntStream.range(0, existingProduct.getFeatures().size())
					.filter(i -> feature.getFeatureId().equals(existingProduct.getFeatures().get(i).getFeatureId()))
					.findFirst();

			if (indexOpt.isEmpty()) {
				throw new RepositoryException(String.format("Feature '%s' not found for product '%s'", feature.getFeatureId(), productId));
			}

			existingProduct.getFeatures().set(indexOpt.getAsInt(), feature);

			return existingProduct;
		});
	}

	@Override
	public void removeFeature(Feature feature) {

		validateFeature(feature);

		String productId = feature.getProduct().getProductId();

		if (!this.products.containsKey(productId)) {
			throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
		}

		this.products.computeIfPresent(productId, (key, existingProduct) -> {

			OptionalInt indexOpt = IntStream.range(0, existingProduct.getFeatures().size())
					.filter(i -> feature.getFeatureId().equals(existingProduct.getFeatures().get(i).getFeatureId()))
					.findFirst();

			if (indexOpt.isEmpty()) {
				throw new RepositoryException(String.format("Feature '%s' not found for product '%s'", feature.getFeatureId(), productId));
			}

			existingProduct.getFeatures().remove(indexOpt.getAsInt());

			return existingProduct;
		});
	}


	public void clear() {
		this.products.clear();
	}

	public void load(InputStream inputStream) throws IOException {

		TypeReference<List<Product>> typeRef = new TypeReference<>() { };
		List<Product> loadedProducts = this.mapper.readValue(inputStream, typeRef);

		loadedProducts.forEach(product -> linkProductFeatures(product));

		this.products = loadedProducts.stream()
				.collect(Collectors.toMap(Product::getProductId, product -> product));
	}

	public void store(OutputStream outputStream) throws IOException {

		this.mapper.writeValue(outputStream, this.products.values());
	}

	private void linkProductFeatures(Product product) {
		List<Feature> updatedFeatures = product.getFeatures().stream()
				.map(f -> {
					Feature newFeature = new Feature(product, f.getFeatureId());
					newFeature.getLimits().addAll(f.getLimits());
					return newFeature;
				})
				.toList();

		product.getFeatures().clear();
		product.getFeatures().addAll(updatedFeatures);
	}


	private void validateProduct(Product product) {

		if (product == null) {
			throw new RepositoryException(("Product must not be 'null'"));
		}

		validateProductId(product.getProductId());
	}

	private void validateProductId(String productId) {

		if (productId == null) {
			throw new RepositoryException(("ProductId must not be 'null'"));
		}
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
}
