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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InMemoryProductRepository}.
 *
 * @author Jean-Yves Desjardins
 * @since 1.0
 */
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

		Optional<UsageLimit> limit = this.repository.getGlobalLimit(f1, "l1");

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

		Optional<UsageLimit> limit = this.repository.getGlobalLimit(f1, "l2");

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

		Optional<UsageLimit> limit = this.repository.getGlobalLimit(f1, "l1");

		assertThat(limit).isPresent();
		assertThat(limit.get().getId()).isEqualTo("l1");
		assertThat(limit.get().getValue()).isEqualTo(10);
	}

	@Test
	void store_repository_to_json() throws IOException {

		this.populateRepository();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.repository.store(baos);
		baos.close();

		DocumentContext dc = JsonPath.parse(baos.toString());

		assertThat((int) dc.read("$[0].length()")).isEqualTo(2);

		assertThat((String) dc.read("$[0].productId")).isEqualTo("Library");
		assertThat((int) dc.read("$[0].features.length()")).isEqualTo(1);

		assertThat((String) dc.read("$[0].features[0].featureId")).isEqualTo("Reserving books");
		assertThat((int) dc.read("$[0].features[0].limits.length()")).isEqualTo(1);

		assertThat((String) dc.read("$[0].features[0].limits[0].type")).isEqualTo("CountLimit");
		assertThat((String) dc.read("$[0].features[0].limits[0].id")).isEqualTo("Maximum books reserved");
		assertThat((int) dc.read("$[0].features[0].limits[0].count")).isEqualTo(5);


		assertThat((String) dc.read("$[1].productId")).isEqualTo("Picture hosting service");
		assertThat((int) dc.read("$[1].features.length()")).isEqualTo(2);

		assertThat((String) dc.read("$[1].features[0].featureId")).isEqualTo("Uploading pictures");
		assertThat((int) dc.read("$[1].features[0].limits.length()")).isEqualTo(2);

		assertThat((String) dc.read("$[1].features[0].limits[0].type")).isEqualTo("CountLimit");
		assertThat((String) dc.read("$[1].features[0].limits[0].id")).isEqualTo("Maximum picture size");
		assertThat((int) dc.read("$[1].features[0].limits[0].count")).isEqualTo(10);
		assertThat((String) dc.read("$[1].features[0].limits[0].unit")).isEqualTo("Go");

		assertThat((String) dc.read("$[1].features[0].limits[1].type")).isEqualTo("SlidingWindowRateLimit");
		assertThat((String) dc.read("$[1].features[0].limits[1].id")).isEqualTo("Maximum of pictures uploaded by hour");
		assertThat((int) dc.read("$[1].features[0].limits[1].quota")).isEqualTo(10);
		assertThat((String) dc.read("$[1].features[0].limits[1].interval")).isEqualTo("HOURS");
		assertThat((int) dc.read("$[1].features[0].limits[1].duration")).isEqualTo(1);

		assertThat((String) dc.read("$[1].features[1].featureId")).isEqualTo("Downloading pictures");
		assertThat((int) dc.read("$[1].features[1].limits.length()")).isEqualTo(2);

		assertThat((String) dc.read("$[1].features[1].limits[0].type")).isEqualTo("SlidingWindowRateLimit");
		assertThat((String) dc.read("$[1].features[1].limits[0].id")).isEqualTo("Maximum of pictures downloaded by hour");
		assertThat((int) dc.read("$[1].features[1].limits[0].quota")).isEqualTo(8);
		assertThat((String) dc.read("$[1].features[1].limits[0].interval")).isEqualTo("MINUTES");
		assertThat((int) dc.read("$[1].features[1].limits[0].duration")).isEqualTo(60);

		assertThat((String) dc.read("$[1].features[1].limits[1].type")).isEqualTo("CalendarPeriodRateLimit");
		assertThat((String) dc.read("$[1].features[1].limits[1].id")).isEqualTo("Maximum of pictures downloaded by calendar month");
		assertThat((int) dc.read("$[1].features[1].limits[1].quota")).isEqualTo(10);
		assertThat((String) dc.read("$[1].features[1].limits[1].periodicity")).isEqualTo("MONTH");
	}

	@Test
	void load_repository_from_json() throws IOException {

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
		assertThat(uploadingPicture.get().getLimits()).hasSize(2);

		assertThat(uploadingPicture.get().getLimits().get(0)).isInstanceOf(CountLimit.class);
		CountLimit maximumPictureSize = (CountLimit) uploadingPicture.get().getLimits().get(0);
		assertThat(maximumPictureSize.getId()).isEqualTo("Maximum picture size");
		assertThat(maximumPictureSize.getValue()).isEqualTo(10);
		assertThat(maximumPictureSize.getUnit()).isEqualTo("Go");

		assertThat(uploadingPicture.get().getLimits().get(1)).isInstanceOf(SlidingWindowRateLimit.class);
		SlidingWindowRateLimit maximumPicturesInMonth = (SlidingWindowRateLimit) uploadingPicture.get().getLimits().get(1);
		assertThat(maximumPicturesInMonth.getId()).isEqualTo("Maximum of pictures uploaded by hour");
		assertThat(maximumPicturesInMonth.getValue()).isEqualTo(10);
		assertThat(maximumPicturesInMonth.getInterval()).isEqualTo(ChronoUnit.HOURS);
		assertThat(maximumPicturesInMonth.getDuration()).isEqualTo(1);

		Optional<Feature> downloadingPicture = this.repository.getFeature(pictureHostingService, "Downloading pictures");
		assertThat(downloadingPicture).isPresent();
		assertThat(downloadingPicture.get().getFeatureId()).isEqualTo("Downloading pictures");
		assertThat(downloadingPicture.get().getProduct().getProductId()).isEqualTo("Picture hosting service");
		assertThat(downloadingPicture.get().getLimits()).hasSize(2);

		assertThat(downloadingPicture.get().getLimits().get(0)).isInstanceOf(SlidingWindowRateLimit.class);
		maximumPicturesInMonth = (SlidingWindowRateLimit) downloadingPicture.get().getLimits().get(0);
		assertThat(maximumPicturesInMonth.getId()).isEqualTo("Maximum of pictures downloaded by hour");
		assertThat(maximumPicturesInMonth.getValue()).isEqualTo(8);
		assertThat(maximumPicturesInMonth.getInterval()).isEqualTo(ChronoUnit.MINUTES);
		assertThat(maximumPicturesInMonth.getDuration()).isEqualTo(60);

		assertThat(downloadingPicture.get().getLimits().get(1)).isInstanceOf(CalendarPeriodRateLimit.class);
		CalendarPeriodRateLimit maximumPicturesInCalendarMonth = (CalendarPeriodRateLimit) downloadingPicture.get().getLimits().get(1);
		assertThat(maximumPicturesInCalendarMonth.getId()).isEqualTo("Maximum of pictures downloaded by calendar month");
		assertThat(maximumPicturesInCalendarMonth.getValue()).isEqualTo(10);
		assertThat(maximumPicturesInCalendarMonth.getPeriodicity()).isEqualTo(CalendarPeriodRateLimit.Periodicity.MONTH);


		Product library = new Product("Library");

		Optional<Feature> reservingBooks = this.repository.getFeature(library, "Reserving books");
		assertThat(reservingBooks).isPresent();
		assertThat(reservingBooks.get().getFeatureId()).isEqualTo("Reserving books");
		assertThat(reservingBooks.get().getProduct().getProductId()).isEqualTo("Library");
		assertThat(reservingBooks.get().getLimits()).hasSize(1);

		assertThat(reservingBooks.get().getLimits().get(0)).isInstanceOf(CountLimit.class);
		CountLimit maximumBooksReserved = (CountLimit) reservingBooks.get().getLimits().get(0);
		assertThat(maximumBooksReserved.getId()).isEqualTo("Maximum books reserved");
		assertThat(maximumBooksReserved.getValue()).isEqualTo(5);
		assertThat(maximumBooksReserved.getUnit()).isNull();
	}

	private void populateRepository() {

		Product pictureHostingService = new Product("Picture hosting service");
		this.repository.addProduct(pictureHostingService);

		Feature uploadingPicture = new Feature(pictureHostingService, "Uploading pictures");
		this.repository.addFeature(uploadingPicture);

		CountLimit maximumPictureSize = new CountLimit("Maximum picture size", 10);
		maximumPictureSize.setUnit("Go");
		uploadingPicture.getLimits().add(maximumPictureSize);

		SlidingWindowRateLimit maximumPicturesUploadedByHour = new SlidingWindowRateLimit("max-photos-uploaded", 10, ChronoUnit.HOURS, 1);
		maximumPicturesUploadedByHour.setId("Maximum of pictures uploaded by hour");
		uploadingPicture.getLimits().add(maximumPicturesUploadedByHour);

		Feature downloadingPicture = new Feature(pictureHostingService, "Downloading pictures");
		this.repository.addFeature(downloadingPicture);

		SlidingWindowRateLimit maximumPicturesDownloadedByHour = new SlidingWindowRateLimit("max-photos-downloaded", 8, ChronoUnit.MINUTES, 60);
		maximumPicturesDownloadedByHour.setId("Maximum of pictures downloaded by hour");
		downloadingPicture.getLimits().add(maximumPicturesDownloadedByHour);

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
