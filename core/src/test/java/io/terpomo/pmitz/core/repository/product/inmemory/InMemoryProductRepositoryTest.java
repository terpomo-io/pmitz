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

package io.terpomo.pmitz.core.repository.product.inmemory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryProductRepositoryTest {

	InMemoryProductRepository repository = new InMemoryProductRepository();

	@BeforeEach
	void setUp() {
		repository.clear();
	}

	// P R O D U C T

	@Test
	void getProductIds_emptyList() {

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertTrue(ids.isEmpty());
	}

	@Test
	void getProductIds_1ItemList() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertEquals(1, ids.size());
	}

	@Test
	void getProductIds_multipleItemList() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Product p2 = new Product("p2");
		repository.addProduct(p2);
		Product p3 = new Product("p3");
		repository.addProduct(p3);

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertEquals(3, ids.size());
		assertTrue(ids.contains(p1.getProductId()));
		assertTrue(ids.contains(p2.getProductId()));
		assertTrue(ids.contains(p3.getProductId()));
	}

	@Test
	void getProductById_productIdNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getProductById(null);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void getProductById_existingProduct() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");

		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());
	}

	@Test
	void getProductById_notExistingProduct() {

		Optional<Product> product = repository.getProductById("p1");

		assertTrue(product.isEmpty());
	}

	@Test
	void addProduct_productNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void addProduct_invalidProduct() {

		Product p1 = new Product(null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void addProduct_newProduct() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");

		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());
	}

	@Test
	void addProduct_existingProduct() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(p1);
		});

		assertEquals("Product 'p1' already exists", exception.getMessage());
	}

	@Test
	void removeProduct_productNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void removeProduct_invalidProduct() {

		Product p1 = new Product(null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void removeProduct_productNotFound() {

		Product p1 = new Product("p1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(p1);
		});

		assertEquals("Product 'p1' not found", exception.getMessage());
	}

	@Test
	void removeProduct_existingProduct() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		repository.removeProduct(p1);

		Optional<Product> p = repository.getProductById("p1");

		assertTrue(p.isEmpty());
	}


	// F E A T U R E

	@Test
	void addFeature_featureNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(null);
		});

		assertEquals("Feature must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeature_invalidFeatureId() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1,null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("FeatureId must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeature_invalidProductId() {

		Product p1 = new Product(null);
		Feature f1 = new Feature(p1,"f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeature_productNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1,"f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("Product 'p1' not found", exception.getMessage());
	}

	@Test
	void addFeature_validFeature() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit cl1 = new CountLimit("cl1", 10);
		f1.getLimits().add(cl1);

		repository.addFeature(f1);

		Optional<Feature> f1p = repository.getFeature(p1, "f1");

		assertTrue(f1p.isPresent());
		assertEquals(f1, f1p.get());
	}

	@Test
	void addFeature_existingFeature() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		repository.addFeature(f1);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("Feature 'f1' already exists", exception.getMessage());
	}

	@Test
	void getFeature_invalidProduct() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeature(null, "f1");
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeature_invalidFeatureId() {

		Product p1 = new Product("p1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeature(p1, null);
		});

		assertEquals("FeatureId must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeature_ProductNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		Optional<Feature> feature = repository.getFeature(p1, f1.getFeatureId());

		assertTrue(feature.isEmpty());
	}

	@Test
	void getFeatures_ExistingFeaturesList() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		repository.addFeature(f2);

		List<Feature> featuresList = repository.getFeatures(p1);

		assertNotNull(featuresList);
		assertEquals(2, featuresList.size());
		assertTrue(featuresList.contains(f1));
		assertTrue(featuresList.contains(f2));
	}

	@Test
	void getFeatures_emptyFeaturesList() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		List<Feature> featuresList = repository.getFeatures(p1);

		assertNotNull(featuresList);
		assertEquals(0, featuresList.size());
	}

	@Test
	void getFeatures_invalidProduct() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeatures(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeatures_invalidProductId() {

		Product p1 = new Product(null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeatures(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeature_ProductNotFound() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(f1);
		});

		assertEquals("Product 'p1' not found", exception.getMessage());
	}

	@Test
	void updateFeature_FeatureNotFound() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(f1);
		});

		assertEquals("Feature 'f1' not found for product 'p1'", exception.getMessage());
	}

	@Test
	void updateFeature_invalidFeature() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(null);
		});

		assertEquals("Feature must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeature_featureExist() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);

		Feature f1 = new Feature(p1, "f1");
		repository.addFeature(f1);
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);

		Feature f2 = new Feature(p1, "f2");
		repository.addFeature(f2);


		repository.updateFeature(f2);

		Optional<Feature> f = repository.getFeature(f2.getProduct(), f2.getFeatureId());

		assertTrue(f.isPresent());
		assertEquals(0, f.get().getLimits().size());
	}

	@Test
	void removeFeature_featureExist() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		repository.addFeature(f1);
		Feature f2 = new Feature(p1, "f2");
		repository.addFeature(f2);

		repository.removeFeature(f1);

		Optional<Feature> f = repository.getFeature(f1.getProduct(), f1.getFeatureId());

		assertTrue(f.isEmpty());

		Optional<Product> op = repository.getProductById("p1");
		assertTrue(op.isPresent());

		p1 = op.get();
		assertEquals(1, p1.getFeatures().size());
	}

	@Test
	void removeFeature_productNotFounc() {

		Product p1 = new Product("p1");
		Feature f1 = new Feature(p1, "f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeFeature(f1);
		});

		assertEquals("Product 'p1' not found", exception.getMessage());
	}

	@Test
	void removeFeature_featureNotFounc() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeFeature(f1);
		});

		assertEquals("Feature 'f1' not found for product 'p1'", exception.getMessage());
	}

	@Test
	void getGlobalList_emptyLimitList() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		repository.addFeature(f1);

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l1");

		assertTrue(limit.isEmpty());
	}

	@Test
	void getGlobalList_LimitNotFound() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);
		repository.addFeature(f1);

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l2");

		assertTrue(limit.isEmpty());
	}

	@Test
	void getGlobalList_LimitFound() {

		Product p1 = new Product("p1");
		repository.addProduct(p1);
		Feature f1 = new Feature(p1, "f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.getLimits().add(l1);
		repository.addFeature(f1);

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l1");

		assertTrue(limit.isPresent());
		assertEquals("l1", limit.get().getId());
	}

	@Test
	@Order(1)
	void store_withFeatures() throws IOException {

		// P R O D U C T :  P I C T U R E   H O S T I N G   S E R V I C E
		Product pictureHostingService = new Product("Picture hosting service");
		repository.addProduct(pictureHostingService);

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Feature uploadingPicture = new Feature(pictureHostingService, "Uploading pictures");
		repository.addFeature(uploadingPicture);

		// C O U N T L I M I T
		CountLimit maximumPictureSize = new CountLimit("Maximum picture size", 10);
		maximumPictureSize.setUnit("Go");
		uploadingPicture.getLimits().add(maximumPictureSize);

		// R A T E L I M I T
		SlidingWindowRateLimit maximumPicturesUploadedByHour = new SlidingWindowRateLimit("max-photos-uploaded", 10, ChronoUnit.HOURS, 1);
		maximumPicturesUploadedByHour.setId("Maximum of pictures uploaded by hour");
		uploadingPicture.getLimits().add(maximumPicturesUploadedByHour);

		// F E A T U R E :  D O W N L O A D I N G   P I C T U R E S
		Feature downloadingPicture = new Feature(pictureHostingService, "Downloading pictures");
		repository.addFeature(downloadingPicture);

		// R A T E L I M I T ( S l i d i n g W i n d o w R a t e L i m i t )
		SlidingWindowRateLimit maximumPicturesDownloadedByHour = new SlidingWindowRateLimit("max-photos-downloaded", 8, ChronoUnit.MINUTES, 60);
		maximumPicturesDownloadedByHour.setId("Maximum of pictures downloaded by hour");
		downloadingPicture.getLimits().add(maximumPicturesDownloadedByHour);

		// R A T E L I M I T ( C a l e n d a r P e r i o d R a t e L i m i t )
		CalendarPeriodRateLimit maximumPicturesDownloadedByCalendarMonth = new CalendarPeriodRateLimit("max-photos-downloaded-by-calendar-month", 10, CalendarPeriodRateLimit.Periodicity.MONTH);
		maximumPicturesDownloadedByCalendarMonth.setId("Maximum of pictures downloaded by calendar month");
		downloadingPicture.getLimits().add(maximumPicturesDownloadedByCalendarMonth);


		// P R O D U C T :  L I B R A R Y
		Product lendingBooks = new Product("Library");
		repository.addProduct(lendingBooks);

		// F E A T U R E :  R E S E R V I N G   B O O K S
		Feature reservingBooks = new Feature(lendingBooks, "Reserving books");
		repository.addFeature(reservingBooks);

		// C O U N T L I M I T
		CountLimit maximumBooksReserved = new CountLimit("Maximum books reserved", 5);
		reservingBooks.getLimits().add(maximumBooksReserved);


		FileOutputStream fos = new FileOutputStream("products_repository.json");
		repository.store(fos);
		fos.close();
	}

	@Test
	@Order(1)
	void load_jsonWithFeatures() throws IOException {

		FileInputStream fis = new FileInputStream("products_repository.json");
		repository.load(fis);
		fis.close();

		// P R O D U C T :  P I C T U R E   H O S T I N G   S E R V I C E
		Product pictureHostingService = new Product("Picture hosting service");

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Optional<Feature> uploadingPicture = repository.getFeature(pictureHostingService, "Uploading pictures");
		assertTrue(uploadingPicture.isPresent());
		assertEquals("Uploading pictures", uploadingPicture.get().getFeatureId());
		assertEquals("Picture hosting service", uploadingPicture.get().getProduct().getProductId());
		assertEquals(2, uploadingPicture.get().getLimits().size());

		// C O U N T L I M I T
		assertTrue(uploadingPicture.get().getLimits().get(0) instanceof CountLimit);
		CountLimit maximumPictureSize = (CountLimit)uploadingPicture.get().getLimits().get(0);
		assertEquals("Maximum picture size", maximumPictureSize.getId());
		assertEquals(10, maximumPictureSize.getValue());
		assertEquals("Go", maximumPictureSize.getUnit());

		// R A T E L I M I T
		assertTrue(uploadingPicture.get().getLimits().get(1) instanceof SlidingWindowRateLimit);
		SlidingWindowRateLimit maximumPicturesInMonth = (SlidingWindowRateLimit)uploadingPicture.get().getLimits().get(1);
		assertEquals("Maximum of pictures uploaded by hour", maximumPicturesInMonth.getId());
		assertEquals(10, maximumPicturesInMonth.getValue());
		assertEquals(ChronoUnit.HOURS, maximumPicturesInMonth.getInterval());
		assertEquals(1, maximumPicturesInMonth.getDuration());

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Optional<Feature> downloadingPicture = repository.getFeature(pictureHostingService, "Downloading pictures");
		assertTrue(downloadingPicture.isPresent());
		assertEquals("Downloading pictures", downloadingPicture.get().getFeatureId());
		assertEquals("Picture hosting service", downloadingPicture.get().getProduct().getProductId());
		assertEquals(2, downloadingPicture.get().getLimits().size());

		// R A T E L I M I T ( S l i d i n g W i n d o w R a t e L i m i t )
		assertTrue(downloadingPicture.get().getLimits().get(0) instanceof SlidingWindowRateLimit);
		maximumPicturesInMonth = (SlidingWindowRateLimit)downloadingPicture.get().getLimits().get(0);
		assertEquals("Maximum of pictures downloaded by hour", maximumPicturesInMonth.getId());
		assertEquals(8, maximumPicturesInMonth.getValue());
		assertEquals(ChronoUnit.MINUTES, maximumPicturesInMonth.getInterval());
		assertEquals(60, maximumPicturesInMonth.getDuration());

		// R A T E L I M I T ( C a l e n d a r P e r i o d R a t e L i m i t )
		assertTrue(downloadingPicture.get().getLimits().get(1) instanceof CalendarPeriodRateLimit);
		CalendarPeriodRateLimit maximumPicturesInCalendarMonth = (CalendarPeriodRateLimit)downloadingPicture.get().getLimits().get(1);
		assertEquals("Maximum of pictures downloaded by calendar month", maximumPicturesInCalendarMonth.getId());
		assertEquals(10, maximumPicturesInCalendarMonth.getValue());
		assertEquals(CalendarPeriodRateLimit.Periodicity.MONTH, maximumPicturesInCalendarMonth.getPeriodicity());


		// P R O D U C T :  L I B R A R Y
		Product library = new Product("Library");

		// F E A T U R E :  R E S E R V I N G   B O O K S
		Optional<Feature> reservingBooks = repository.getFeature(library, "Reserving books");
		assertTrue(reservingBooks.isPresent());
		assertEquals("Reserving books", reservingBooks.get().getFeatureId());
		assertEquals("Library", reservingBooks.get().getProduct().getProductId());
		assertEquals(1, reservingBooks.get().getLimits().size());

		// C O U N T L I M I T
		assertTrue(reservingBooks.get().getLimits().get(0) instanceof CountLimit);
		CountLimit maximumBooksReserved = (CountLimit)reservingBooks.get().getLimits().get(0);
		assertEquals("Maximum books reserved", maximumBooksReserved.getId());
		assertEquals(5, maximumBooksReserved.getValue());
		assertNull(maximumBooksReserved.getUnit());
	}
}
