/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InMemoryProductRepository}.
 *
 * @author Jean-Yves Desjardins
 * @since 1.0
 */
@TestMethodOrder(OrderAnnotation.class)
class InMemoryProductRepositoryTests {

	private InMemoryProductRepository repository;


	@BeforeEach
	void setUp() {
		this.repository = new InMemoryProductRepository();
	}

	@Test
	void getProductIds_emptyList() {

		List<String> ids = this.repository.getProductIds();

		assertThat(ids).isNotNull().isEmpty();
	}

	@Test
	void getProductIds_1ItemList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		List<String> ids = this.repository.getProductIds();

		assertThat(ids).isNotNull()
				.hasSize(1);
	}

	@Test
	void getProductIds_multipleItemList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Product p2 = new Product("p2");
		this.repository.addProduct(p2);
		Product p3 = new Product("p3");
		this.repository.addProduct(p3);

		List<String> ids = this.repository.getProductIds();

		assertThat(ids).isNotNull()
				.hasSize(3)
				.contains(p1.getProductId())
				.contains(p2.getProductId())
				.contains(p3.getProductId());
	}

	@Test
	void getProductById_productIdNull() {

		assertThatExceptionOfType(RepositoryException.class)
				.isThrownBy(() -> this.repository.getProductById(null))
				.withMessage("ProductId must not be 'null'");
	}

	@Test
	void getProductById_existingProduct() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		Optional<Product> p = this.repository.getProductById("p1");

		assertThat(p).isPresent();
		assertThat(p.get().getProductId()).isEqualTo(p1.getProductId());
	}

	@Test
	void getProductById_notExistingProduct() {

		Optional<Product> product = this.repository.getProductById("p1");

		assertThat(product).isEmpty();
	}

	@Test
	void addProduct_productNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addProduct(null))
				.withMessage("Product must not be 'null'");
	}

	@Test
	void addProduct_invalidProduct() {

		Product p1 = new Product(null);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addProduct(p1))
				.withMessage("ProductId must not be 'null'");
	}

	@Test
	void addProduct_newProduct() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		Optional<Product> p = this.repository.getProductById("p1");

		assertThat(p).isPresent();
		assertThat(p.get().getProductId()).isEqualTo(p1.getProductId());
	}

	@Test
	void addProduct_existingProduct() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addProduct(p1))
				.withMessage("Product 'p1' already exists");
	}

	@Test
	void removeProduct_productNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removeProduct(null))
				.withMessage("Product must not be 'null'");
	}

	@Test
	void removeProduct_invalidProduct() {

		Product p1 = new Product(null);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removeProduct(p1))
						.withMessage("ProductId must not be 'null'");
	}

	@Test
	void removeProduct_productNotFound() {

		Product p1 = new Product("p1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removeProduct(p1))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void removeProduct_existingProduct() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		this.repository.removeProduct(p1);

		Optional<Product> p = this.repository.getProductById("p1");

		assertThat(p).isNotPresent();
	}

	@Test
	void addFeature_featureNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addFeature(null))
				.withMessage("Feature must not be 'null'");
	}

	@Test
	void addFeature_invalidFeatureId() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, null);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addFeature(f1))
				.withMessage("FeatureId must not be 'null'");
	}

	@Test
	void addFeature_invalidProductId() {

		Product p1 = new Product(null);
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addFeature(f1))
				.withMessage("ProductId must not be 'null'");
	}

	@Test
	void addFeature_productNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addFeature(f1))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void addFeature_validFeature() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit cl1 = new CountLimit("cl1", 10);
		f1.getLimits().add(cl1);

		this.repository.addFeature(f1);

		Optional<Feature> f1p = this.repository.getFeature(p1, "f1");

		assertThat(f1p).isPresent().contains(f1);
	}

	@Test
	void addFeature_existingFeature() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		this.repository.addFeature(f1);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addFeature(f1))
				.withMessage("Feature 'f1' already exists");
	}

	@Test
	void getFeature_invalidProduct() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.getFeature(null, "f1"))
				.withMessage("Product must not be 'null'");
	}

	@Test
	void getFeature_invalidFeatureId() {

		Product p1 = new Product("p1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.getFeature(p1, null))
				.withMessage("FeatureId must not be 'null'");
	}

	@Test
	void getFeature_ProductNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		Optional<Feature> feature = this.repository.getFeature(p1, f1.getFeatureId());

		assertThat(feature).isNotPresent();
	}

	@Test
	void getFeatures_ExistingFeaturesList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);

		List<Feature> featuresList = this.repository.getFeatures(p1);

		assertThat(featuresList).isNotNull().hasSize(2).contains(f1, f2);
	}

	@Test
	void getFeatures_emptyFeaturesList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		List<Feature> featuresList = this.repository.getFeatures(p1);

		assertThat(featuresList).isNotNull().isEmpty();
	}

	@Test
	void getFeatures_invalidProduct() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.getFeatures(null))
				.withMessage("Product must not be 'null'");
	}

	@Test
	void getFeatures_invalidProductId() {

		Product p1 = new Product(null);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.getFeatures(p1))
				.withMessage("ProductId must not be 'null'");
	}

	@Test
	void updateFeature_ProductNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updateFeature(f1))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void updateFeature_FeatureNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updateFeature(f1))
				.withMessage("Feature 'f1' not found for product 'p1'");
	}

	@Test
	void updateFeature_invalidFeature() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updateFeature(null))
				.withMessage("Feature must not be 'null'");
	}

	@Test
	void updateFeature_featureExist() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);

		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);


		this.repository.updateFeature(f2);

		Optional<Feature> f = this.repository.getFeature(f2.getProduct(), f2.getFeatureId());

		assertThat(f).isPresent();
		assertThat(f.get().getLimits()).isEmpty();
	}

	@Test
	void removeFeature_featureExist() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);

		this.repository.removeFeature(f1);

		Optional<Feature> f = this.repository.getFeature(f1.getProduct(), f1.getFeatureId());

		assertThat(f).isEmpty();

		Optional<Product> op = this.repository.getProductById("p1");
		assertThat(op).isPresent();

		p1 = op.get();
		assertThat(p1.getFeatures()).hasSize(1);
	}

	@Test
	void removeFeature_productNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removeFeature(f1))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void removeFeature_featureNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removeFeature(f1))
				.withMessage("Feature 'f1' not found for product 'p1'");
	}

	@Test
	void getGlobalLimit_emptyLimitList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);

		Optional<LimitRule> limit = this.repository.getGlobalLimit(f1, "l1");

		assertThat(limit).isNotPresent();
	}

	@Test
	void getGlobalLimit_LimitNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);
		this.repository.addFeature(f1);

		Optional<LimitRule> limit = this.repository.getGlobalLimit(f1, "l2");

		assertThat(limit).isEmpty();
	}

	@Test
	void getGlobalLimit_LimitFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);
		this.repository.addFeature(f1);

		Optional<LimitRule> limit = this.repository.getGlobalLimit(f1, "l1");

		assertThat(limit).isPresent();
		assertThat(limit.get().getId()).isEqualTo("l1");
		assertThat(limit.get().getValue()).isEqualTo(10);
	}

	@Test
	@Order(1)
	void store_products_and_features_to_json() throws IOException {

		this.populateRepository();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.repository.store(baos);
		baos.close();

		DocumentContext dc = JsonPath.parse(baos.toString());

		assertThat((int) dc.read("$[0].length()")).isEqualTo(3);

		assertThat((String) dc.read("$[0].productId")).isEqualTo("Library");
		assertThat((int) dc.read("$[0].features.length()")).isEqualTo(1);

		assertThat((String) dc.read("$[0].features[0].featureId")).isEqualTo("Reserving books");
		assertThat((int) dc.read("$[0].features[0].limits.length()")).isEqualTo(1);

		assertThat((String) dc.read("$[1].productId")).isEqualTo("Picture hosting service");
		assertThat((int) dc.read("$[1].features.length()")).isEqualTo(2);

		assertThat((String) dc.read("$[1].features[0].featureId")).isEqualTo("Uploading pictures");
		assertThat((int) dc.read("$[1].features[0].limits.length()")).isEqualTo(1);

		assertThat((String) dc.read("$[1].features[1].featureId")).isEqualTo("Downloading pictures");
		assertThat((int) dc.read("$[1].features[1].limits.length()")).isEqualTo(2);
	}

	@Test
	@Order(2)
	void store_limits_to_json() throws IOException {

		this.populateRepository();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.repository.store(baos);
		baos.close();

		DocumentContext dc = JsonPath.parse(baos.toString());

		assertThat((String) dc.read("$[0].features[0].limits[0].type")).isEqualTo("CountLimit");
		assertThat((String) dc.read("$[0].features[0].limits[0].id")).isEqualTo("Maximum books reserved");
		assertThat((int) dc.read("$[0].features[0].limits[0].count")).isEqualTo(5);


		assertThat((String) dc.read("$[1].features[0].limits[0].type")).isEqualTo("CountLimit");
		assertThat((String) dc.read("$[1].features[0].limits[0].id")).isEqualTo("Maximum picture size");
		assertThat((int) dc.read("$[1].features[0].limits[0].count")).isEqualTo(10);
		assertThat((String) dc.read("$[1].features[0].limits[0].unit")).isEqualTo("Go");

		assertThat((String) dc.read("$[1].features[1].featureId")).isEqualTo("Downloading pictures");
		assertThat((int) dc.read("$[1].features[1].limits.length()")).isEqualTo(2);

		assertThat((String) dc.read("$[1].features[1].limits[0].type")).isEqualTo("CountLimit");
		assertThat((String) dc.read("$[1].features[1].limits[0].id")).isEqualTo("Maximum picture size");
		assertThat((int) dc.read("$[1].features[1].limits[0].count")).isEqualTo(20);
		assertThat((String) dc.read("$[1].features[1].limits[0].unit")).isEqualTo("Go");

		assertThat((String) dc.read("$[1].features[1].limits[1].type")).isEqualTo("CalendarPeriodRateLimit");
		assertThat((String) dc.read("$[1].features[1].limits[1].id")).isEqualTo("Maximum of pictures downloaded by calendar month");
		assertThat((int) dc.read("$[1].features[1].limits[1].quota")).isEqualTo(10);
		assertThat((String) dc.read("$[1].features[1].limits[1].periodicity")).isEqualTo("MONTH");
	}

	@Test
	@Order(10)
	void load_products_and_features_from_json() throws IOException {

		InputStream ioStream = this.getClass()
				.getClassLoader()
				.getResourceAsStream("products_repository.json");
		this.repository.load(ioStream);
		if (ioStream != null) {
			ioStream.close();
		}

		Product pictureHostingService = new Product("Picture hosting service");

		Optional<Feature> uploadingPicture = this.repository.getFeature(pictureHostingService, "Uploading pictures");
		assertThat(uploadingPicture).isPresent();
		assertThat(uploadingPicture.get().getFeatureId()).isEqualTo("Uploading pictures");
		assertThat(uploadingPicture.get().getProduct().getProductId()).isEqualTo("Picture hosting service");
		assertThat(uploadingPicture.get().getLimits()).hasSize(1);

		Optional<Feature> downloadingPicture = this.repository.getFeature(pictureHostingService, "Downloading pictures");
		assertThat(downloadingPicture).isPresent();
		assertThat(downloadingPicture.get().getFeatureId()).isEqualTo("Downloading pictures");
		assertThat(downloadingPicture.get().getProduct().getProductId()).isEqualTo("Picture hosting service");
		assertThat(downloadingPicture.get().getLimits()).hasSize(2);


		Product library = new Product("Library");

		Optional<Feature> reservingBooks = this.repository.getFeature(library, "Reserving books");
		assertThat(reservingBooks).isPresent();
		assertThat(reservingBooks.get().getFeatureId()).isEqualTo("Reserving books");
		assertThat(reservingBooks.get().getProduct().getProductId()).isEqualTo("Library");
		assertThat(reservingBooks.get().getLimits()).hasSize(1);
	}

	@Test
	@Order(11)
	void load_limits_from_json() throws IOException {

		InputStream ioStream = this.getClass()
				.getClassLoader()
				.getResourceAsStream("products_repository.json");
		this.repository.load(ioStream);
		if (ioStream != null) {
			ioStream.close();
		}

		Product pictureHostingService = repository.getProductById("Picture hosting service").orElseThrow();

		Optional<Feature> uploadingPicture = this.repository.getFeature(pictureHostingService, "Uploading pictures");
		assertThat(uploadingPicture).isPresent();
		assertThat(uploadingPicture.get().getLimits().get(0)).isInstanceOf(CountLimit.class);
		CountLimit maximumPictureSize = (CountLimit) uploadingPicture.get().getLimits().get(0);
		assertThat(maximumPictureSize.getId()).isEqualTo("Maximum picture size");
		assertThat(maximumPictureSize.getValue()).isEqualTo(10);
		assertThat(maximumPictureSize.getUnit()).isEqualTo("Go");

		Optional<Feature> downloadingPicture = this.repository.getFeature(pictureHostingService, "Downloading pictures");
		assertThat(downloadingPicture).isPresent();

		assertThat(downloadingPicture.get().getLimits().get(0)).isInstanceOf(CountLimit.class);
		maximumPictureSize = (CountLimit) downloadingPicture.get().getLimits().get(0);
		assertThat(maximumPictureSize.getId()).isEqualTo("Maximum picture size");
		assertThat(maximumPictureSize.getValue()).isEqualTo(20);
		assertThat(maximumPictureSize.getUnit()).isEqualTo("Go");

		assertThat(downloadingPicture.get().getLimits().get(1)).isInstanceOf(CalendarPeriodRateLimit.class);
		CalendarPeriodRateLimit maximumPicturesInCalendarMonth = (CalendarPeriodRateLimit) downloadingPicture.get().getLimits().get(1);
		assertThat(maximumPicturesInCalendarMonth.getId()).isEqualTo("Maximum of pictures downloaded by calendar month");
		assertThat(maximumPicturesInCalendarMonth.getValue()).isEqualTo(10);
		assertThat(maximumPicturesInCalendarMonth.getPeriodicity()).isEqualTo(CalendarPeriodRateLimit.Periodicity.MONTH);
	}

	@Test
	@Order(12)
	void load_plans_from_json() throws IOException {

		InputStream ioStream = this.getClass()
				.getClassLoader()
				.getResourceAsStream("products_repository.json");
		this.repository.load(ioStream);
		if (ioStream != null) {
			ioStream.close();
		}

		Product pictureHostingProduct = repository.getProductById("Picture hosting service").orElseThrow();

		var plans = pictureHostingProduct.getPlans();

		assertThat(plans).hasSize(3)
				.anyMatch(plan -> plan.getPlanId().equals("basic") && plan.getDescription().equals("Basic plan"))
				.anyMatch(plan -> plan.getPlanId().equals("standard") && plan.getDescription().equals("Standard plan"))
				.anyMatch(plan -> plan.getPlanId().equals("premium") && plan.getDescription().equals("Premium plan"));


		var basicPlan = plans.stream().filter(plan -> plan.getPlanId().equals("basic")).findFirst();
		assertThat(basicPlan).isPresent();
		assertThat(basicPlan.get().getIncludedFeatures())
				.hasSize(2)
				.containsAll(List.of(pictureHostingProduct.getFeature("Downloading pictures").orElseThrow(), pictureHostingProduct.getFeature("Uploading pictures").orElseThrow()));

		var premiumPlan = plans.stream().filter(plan -> plan.getPlanId().equals("premium")).findFirst();
		assertThat(premiumPlan).isPresent();
		assertThat(premiumPlan.get().getIncludedFeatures())
				.hasSize(2)
				.containsAll(List.of(pictureHostingProduct.getFeature("Downloading pictures").orElseThrow(), pictureHostingProduct.getFeature("Uploading pictures").orElseThrow()));

		assertThat(premiumPlan.get().getLimitsOverride())
				.hasSize(1)
				.anyMatch(CalendarPeriodRateLimit.class::isInstance);

		var calendarLimitRuleoverride = (CalendarPeriodRateLimit) premiumPlan.get().getLimitsOverride().get(0);

		assertThat(calendarLimitRuleoverride.getPeriodicity()).isEqualTo((CalendarPeriodRateLimit.Periodicity.MONTH));
		assertThat(calendarLimitRuleoverride.getId()).isEqualTo(("dwnl-num-pics-month"));
		assertThat(calendarLimitRuleoverride.getValue()).isEqualTo(50);

	}

	// Plan tests

	@Test
	void addPlan_planNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addPlan(null))
				.withMessage("Plan must not be 'null'");
	}

	@Test
	void addPlan_invalidPlanId() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan = new Plan(p1, null, List.of("f1"));

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addPlan(plan))
				.withMessage("PlanId must not be 'null'");
	}

	@Test
	void addPlan_invalidProductId() {

		Product p1 = new Product(null);
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addPlan(plan))
				.withMessage("ProductId must not be 'null'");
	}

	@Test
	void addPlan_productNotFound() {

		Product p1 = new Product("p1");
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addPlan(plan))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void addPlan_validPlan() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan = new Plan(p1, "plan1", List.of("f1"));

		this.repository.addPlan(plan);

		Optional<Plan> retrievedPlan = this.repository.getPlan(p1, "plan1");

		assertThat(retrievedPlan).isPresent();
		assertThat(retrievedPlan.get().getPlanId()).isEqualTo("plan1");
	}

	@Test
	void addPlan_existingPlan() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan = new Plan(p1, "plan1", List.of("f1"));

		this.repository.addPlan(plan);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.addPlan(plan))
				.withMessage("Plan 'plan1' already exists");
	}

	@Test
	void updatePlan_planNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updatePlan(null))
				.withMessage("Plan must not be 'null'");
	}

	@Test
	void updatePlan_productNotFound() {

		Product p1 = new Product("p1");
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updatePlan(plan))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void updatePlan_planNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.updatePlan(plan))
				.withMessage("Plan 'plan1' not found for product 'p1'");
	}

	@Test
	void updatePlan_planExists() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);

		Plan plan = new Plan(p1, "plan1", List.of("f1"));
		this.repository.addPlan(plan);

		Plan updatedPlan = new Plan(p1, "plan1", List.of("f1", "f2"));
		updatedPlan.setDescription("Updated description");

		this.repository.updatePlan(updatedPlan);

		Optional<Plan> retrievedPlan = this.repository.getPlan(p1, "plan1");

		assertThat(retrievedPlan).isPresent();
		assertThat(retrievedPlan.get().getDescription()).isEqualTo("Updated description");
		assertThat(retrievedPlan.get().getIncludedFeatures()).hasSize(2);
	}

	@Test
	void removePlan_planNull() {

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removePlan(null))
				.withMessage("Plan must not be 'null'");
	}

	@Test
	void removePlan_productNotFound() {

		Product p1 = new Product("p1");
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removePlan(plan))
				.withMessage("Product 'p1' not found");
	}

	@Test
	void removePlan_planNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Plan plan = new Plan(p1, "plan1", List.of());

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(() ->
				this.repository.removePlan(plan))
				.withMessage("Plan 'plan1' not found for product 'p1'");
	}

	@Test
	void removePlan_planExists() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan1 = new Plan(p1, "plan1", List.of("f1"));
		this.repository.addPlan(plan1);
		Plan plan2 = new Plan(p1, "plan2", List.of("f1"));
		this.repository.addPlan(plan2);

		this.repository.removePlan(plan1);

		Optional<Plan> removedPlan = this.repository.getPlan(p1, "plan1");
		assertThat(removedPlan).isEmpty();

		List<Plan> remainingPlans = this.repository.getPlans(p1);
		assertThat(remainingPlans).hasSize(1);
		assertThat(remainingPlans.get(0).getPlanId()).isEqualTo("plan2");
	}

	@Test
	void getPlans_emptyPlansList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		List<Plan> plans = this.repository.getPlans(p1);

		assertThat(plans).isNotNull().isEmpty();
	}

	@Test
	void getPlans_existingPlansList() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan1 = new Plan(p1, "plan1", List.of("f1"));
		this.repository.addPlan(plan1);
		Plan plan2 = new Plan(p1, "plan2", List.of("f1"));
		this.repository.addPlan(plan2);

		List<Plan> plans = this.repository.getPlans(p1);

		assertThat(plans).isNotNull().hasSize(2);
	}

	@Test
	void getPlan_planNotFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);

		Optional<Plan> plan = this.repository.getPlan(p1, "plan1");

		assertThat(plan).isEmpty();
	}

	@Test
	void getPlan_planFound() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Plan plan = new Plan(p1, "plan1", List.of("f1"));
		plan.setDescription("Test plan");
		this.repository.addPlan(plan);

		Optional<Plan> retrievedPlan = this.repository.getPlan(p1, "plan1");

		assertThat(retrievedPlan).isPresent();
		assertThat(retrievedPlan.get().getPlanId()).isEqualTo("plan1");
		assertThat(retrievedPlan.get().getDescription()).isEqualTo("Test plan");
	}

	@Test
	void isFeatureIncluded_featureIncluded() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);
		Plan plan = new Plan(p1, "plan1", List.of("f1"));
		this.repository.addPlan(plan);

		boolean result = this.repository.isFeatureIncluded(plan, f1);

		assertThat(result).isTrue();
	}

	@Test
	void isFeatureIncluded_featureNotIncluded() {

		Product p1 = new Product("p1");
		this.repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		this.repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		this.repository.addFeature(f2);
		Plan plan = new Plan(p1, "plan1", List.of("f1"));
		this.repository.addPlan(plan);

		boolean result = this.repository.isFeatureIncluded(plan, f2);

		assertThat(result).isFalse();
	}

	private void populateRepository() {

		Product pictureHostingService = new Product("Picture hosting service");
		this.repository.addProduct(pictureHostingService);

		Feature uploadingPicture = new Feature(pictureHostingService, "Uploading pictures");
		this.repository.addFeature(uploadingPicture);

		CountLimit maximumPictureSize = new CountLimit("Maximum picture size", 10);
		maximumPictureSize.setUnit("Go");
		uploadingPicture.getLimits().add(maximumPictureSize);

		Feature downloadingPicture = new Feature(pictureHostingService, "Downloading pictures");
		this.repository.addFeature(downloadingPicture);

		maximumPictureSize = new CountLimit("Maximum picture size", 20);
		maximumPictureSize.setUnit("Go");
		downloadingPicture.getLimits().add(maximumPictureSize);

		CalendarPeriodRateLimit maximumPicturesDownloadedByCalendarMonth = new CalendarPeriodRateLimit("max-photos-downloaded-by-calendar-month", 10, CalendarPeriodRateLimit.Periodicity.MONTH);
		maximumPicturesDownloadedByCalendarMonth.setId("Maximum of pictures downloaded by calendar month");
		downloadingPicture.getLimits().add(maximumPicturesDownloadedByCalendarMonth);


		Product lendingBooks = new Product("Library");
		this.repository.addProduct(lendingBooks);

		Feature reservingBooks = new Feature(lendingBooks, "Reserving books");
		this.repository.addFeature(reservingBooks);

		CountLimit maximumBooksReserved = new CountLimit("Maximum books reserved", 5);
		reservingBooks.getLimits().add(maximumBooksReserved);
	}
}
