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

package io.terpomo.pmitz.core.repository.product.inmemory;

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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.repository.product.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {

	public static final String PRODUCT_NOT_FOUND = "Product '%s' not found";
	private final ObjectMapper mapper;
	private Map<String, Product> products = new HashMap<>();

	public InMemoryProductRepository() {

		this.mapper = JsonMapper.builder()
				.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
				.enable(SerializationFeature.INDENT_OUTPUT)
				.addMixIn(Product.class, ProductMixIn.class)
				.addMixIn(Feature.class, FeatureMixIn.class)
				.addMixIn(Plan.class, PlanMixIn.class)
				.addMixIn(LimitRule.class, LimitRuleMixIn.class)
				.addMixIn(CalendarPeriodRateLimit.class, CalendarPeriodRateLimitMixIn.class)
				.addMixIn(CountLimit.class, CountLimitMixIn.class)
				.build();
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
	public Optional<LimitRule> getGlobalLimit(Feature feature, String limitRuleId) {

		validateFeature(feature);

		return Optional.ofNullable(feature.getProduct())
				.map(Product::getProductId)
				.flatMap(productId -> Optional.ofNullable(this.products.get(productId)))
				.flatMap(p -> p.getFeatures().stream()
						.filter(feature::equals)
						.findFirst())
				.flatMap(f -> Optional.ofNullable(f.getLimits())
						.flatMap(ll -> ll.stream()
								.filter(l -> l.getId().equals(limitRuleId))
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

	@Override
	public List<Plan> getPlans(Product product) {
		return product.getPlans();
	}

	@Override
	public Optional<Plan> getPlan(Product product, String planId) {
		return product.getPlans().stream().filter(plan -> plan.getPlanId().equals(planId)).findFirst();
	}

	@Override
	public void addPlan(Plan plan) {

		validatePlan(plan);

		String productId = plan.getProduct().getProductId();
		Product existingProduct = this.products.get(productId);

		if (existingProduct == null) {
			throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
		}

		if (existingProduct.getPlans().stream().anyMatch(p -> p.getPlanId().equals(plan.getPlanId()))) {
			throw new RepositoryException(String.format("Plan '%s' already exists", plan.getPlanId()));
		}

		existingProduct.getPlans().add(plan);
	}

	@Override
	public void updatePlan(Plan plan) {

		validatePlan(plan);

		String productId = plan.getProduct().getProductId();

		this.products.compute(productId, (key, existingProduct) -> {

			if (existingProduct == null) {
				throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
			}

			OptionalInt indexOpt = IntStream.range(0, existingProduct.getPlans().size())
					.filter(i -> plan.getPlanId().equals(existingProduct.getPlans().get(i).getPlanId()))
					.findFirst();

			if (indexOpt.isEmpty()) {
				throw new RepositoryException(String.format("Plan '%s' not found for product '%s'", plan.getPlanId(), productId));
			}

			existingProduct.getPlans().set(indexOpt.getAsInt(), plan);

			return existingProduct;
		});
	}

	@Override
	public void removePlan(Plan plan) {

		validatePlan(plan);

		String productId = plan.getProduct().getProductId();

		if (!this.products.containsKey(productId)) {
			throw new RepositoryException(String.format(PRODUCT_NOT_FOUND, productId));
		}

		this.products.computeIfPresent(productId, (key, existingProduct) -> {

			OptionalInt indexOpt = IntStream.range(0, existingProduct.getPlans().size())
					.filter(i -> plan.getPlanId().equals(existingProduct.getPlans().get(i).getPlanId()))
					.findFirst();

			if (indexOpt.isEmpty()) {
				throw new RepositoryException(String.format("Plan '%s' not found for product '%s'", plan.getPlanId(), productId));
			}

			existingProduct.getPlans().remove(indexOpt.getAsInt());

			return existingProduct;
		});
	}

	@Override
	public boolean isFeatureIncluded(Plan plan, Feature feature) {
		return plan.getIncludedFeatures().stream().anyMatch(planInFeature -> planInFeature.getFeatureId().equals(feature.getFeatureId()));
	}


	public void clear() {
		this.products.clear();
	}

	public void load(InputStream inputStream) {

		TypeReference<List<Product>> typeRef = new TypeReference<>() { };
		List<Product> loadedProducts = this.mapper.readValue(inputStream, typeRef);

		loadedProducts.forEach(this::linkProductPlans);

		this.products = loadedProducts.stream()
				.collect(Collectors.toMap(Product::getProductId, product -> product));
	}

	public void store(OutputStream outputStream) {

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

	private void linkProductPlans(Product product) {
		product.getPlans().forEach(plan -> {
			var linkedFeatures = plan.getIncludedFeatures()
							.stream().map(feature -> product.getFeature(feature.getFeatureId())
							.orElseThrow(() -> new IllegalArgumentException("Feature not found with id %s in the product")))
					.collect(Collectors.toSet());
			plan.setIncludedFeatures(linkedFeatures);

		});

	}



	private void validateProduct(Product product) {

		if (product == null) {
			throw new RepositoryException("Product must not be 'null'");
		}

		validateProductId(product.getProductId());
	}

	private void validateProductId(String productId) {

		if (productId == null) {
			throw new RepositoryException("ProductId must not be 'null'");
		}
	}

	private void validateFeature(Feature feature) {

		if (feature == null) {
			throw new RepositoryException("Feature must not be 'null'");
		}
		validateFeatureId(feature.getFeatureId());
		validateProduct(feature.getProduct());
	}

	private void validateFeatureId(String featureId) {

		if (featureId == null) {
			throw new RepositoryException("FeatureId must not be 'null'");
		}
	}

	private void validatePlan(Plan plan) {

		if (plan == null) {
			throw new RepositoryException("Plan must not be 'null'");
		}
		validatePlanId(plan.getPlanId());
		validateProduct(plan.getProduct());
	}

	private void validatePlanId(String planId) {

		if (planId == null) {
			throw new RepositoryException("PlanId must not be 'null'");
		}
	}
}
