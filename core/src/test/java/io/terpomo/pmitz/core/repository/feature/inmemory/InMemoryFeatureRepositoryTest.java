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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.RateLimit;
import io.terpomo.pmitz.core.exception.RepositoryException;
import static org.junit.jupiter.api.Assertions.*;

public class InMemoryFeatureRepositoryTest {

	InMemoryFeatureRepository repository = new InMemoryFeatureRepository();

	@BeforeEach
	public void setUp() {
		repository.clear();
	}

	@Test
	void addFeatures_featureNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(null);
		});

		assertEquals("Feature must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeatures_featureIdNull() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId(null);
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("FeatureId must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeatures_productNull() {

		Feature f1 = new Feature();
		f1.setProduct(null);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void addFeatures_limitsListNull() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(null);

		repository.addFeature(f1);

		Optional<Feature> f1p = repository.getFeature(p1, "f1");

		assertTrue(f1p.isPresent());
		assertEquals(f1, f1p.get());
	}

	@Test
	void addFeatures_featureComplete() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		repository.addFeature(f1);

		Optional<Feature> f1p = repository.getFeature(p1, "f1");

		assertTrue(f1p.isPresent());
		assertEquals(f1, f1p.get());
	}

	@Test
	void addFeatures_featureAlreadyExist() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		repository.addFeature(f1);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addFeature(f1);
		});

		assertEquals("Feature 'f1' already exist", exception.getMessage());
	}

	@Test
	void getFeatures_ExistingFeaturesList() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setFeatureId("f1");
		f1.setProduct(p1);
		f1.setLimits(List.of());
		repository.addFeature(f1);

		Feature f2 = new Feature();
		f2.setFeatureId("f2");
		f2.setProduct(p1);
		f2.setLimits(List.of());
		repository.addFeature(f2);

		List<Feature> featuresList = repository.getFeatures(p1);

		assertNotNull(featuresList);
		assertEquals(2, featuresList.size());
		assertTrue(featuresList.contains(f1));
		assertTrue(featuresList.contains(f2));
	}

	@Test
	void getFeatures_NotExistingFeaturesList() {

		Product p1 = new Product();
		p1.setProductId("p1");

		List<Feature> featuresList = repository.getFeatures(p1);

		assertNotNull(featuresList);
		assertEquals(0, featuresList.size());
	}

	@Test
	void getFeatures_ProductNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeatures(null);
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeatures_ProductIdNull() {

		Product p1 = new Product();
		p1.setProductId(null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeatures(p1);
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeature_ExistingFeature() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setFeatureId("f1");
		f1.setProduct(p1);
		f1.setLimits(List.of());
		repository.addFeature(f1);

		Optional<Feature> f = repository.getFeature(p1, f1.getFeatureId());

		assertTrue(f.isPresent());
		assertEquals(f1.getFeatureId(), f.get().getFeatureId());
		assertEquals(f1.getProduct(), f.get().getProduct());
		assertEquals(f1.getLimits(), f.get().getLimits());
	}

	@Test
	void getFeature_ProductNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeature(null, "f1");
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeature_ProductIdNull() {

		Product p1 = new Product();
		p1.setProductId(null);

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeature(p1, "f1");
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void getFeature_ProductNotFound() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setFeatureId("f1");
		f1.setProduct(p1);
		f1.setLimits(List.of());

		Optional<Feature> feature = repository.getFeature(p1, f1.getFeatureId());

		assertTrue(feature.isEmpty());
	}


	@Test
	void getFeature_FeatureIdNull() {

		Product p1 = new Product();
		p1.setProductId("p1");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getFeature(p1, null);
		});

		assertEquals("FeatureId must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeature_FeatureNotFound() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setFeatureId("f1");
		f1.setProduct(p1);
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(f1);
		});

		assertEquals("Feature 'f1' not found for product 'p1'", exception.getMessage());
	}

	@Test
	void updateFeatures_featureNull() {

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(null);
		});

		assertEquals("Feature must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeatures_featureIdNull() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId(null);
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(f1);
		});

		assertEquals("FeatureId must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeatures_productNull() {

		Feature f1 = new Feature();
		f1.setProduct(null);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateFeature(f1);
		});

		assertEquals("Product or productId must not be 'null'", exception.getMessage());
	}

	@Test
	void updateFeatures_featureExist() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());
		repository.addFeature(f1);

		CountLimit l1 = new CountLimit("l1", 10);
		f1.setLimits(List.of(l1));

		repository.updateFeature(f1);

		Optional<Feature> f = repository.getFeature(f1.getProduct(), f1.getFeatureId());

		assertTrue(f.isPresent());
		assertEquals(1, f.get().getLimits().size());
	}

	@Test
	void removeFeatures_featureExist() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());
		repository.addFeature(f1);

		repository.removeFeature(f1);

		Optional<Feature> f = repository.getFeature(f1.getProduct(), f1.getFeatureId());

		assertTrue(f.isEmpty());
	}

	@Test
	void removeFeatures_featureUnexist() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
					repository.removeFeature(f1);
		});

		assertEquals("Feature 'f1' not found for product 'p1'", exception.getMessage());
	}

	@Test
	void getGlobalList_emptyLimitList() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		f1.setLimits(List.of());

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l1");

		assertTrue(limit.isEmpty());
	}

	@Test
	void getGlobalList_nullLimitList() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l1");

		assertTrue(limit.isEmpty());
	}

	@Test
	void getGlobalList_LimitNotFound() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.setLimits(List.of(l1));

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l2");

		assertTrue(limit.isEmpty());
	}

	@Test
	void getGlobalList_LimitFound() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Feature f1 = new Feature();
		f1.setProduct(p1);
		f1.setFeatureId("f1");
		CountLimit l1 = new CountLimit("l1", 10);
		f1.setLimits(List.of(l1));

		Optional<UsageLimit> limit = repository.getGlobalLimit(f1, "l1");

		assertTrue(limit.isPresent());
		assertEquals("l1", limit.get().getId());
	}

	@Test
	@Order(1)
	void store_withFeatures() throws IOException {

		// P R O D U C T :  P I C T U R E   H O S T I N G   S E R V I C E
		Product pictureHostingService = new Product();
		pictureHostingService.setProductId("Picture hosting service");

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Feature uploadingPicture = new Feature();
		uploadingPicture.setFeatureId("Uploading pictures");
		uploadingPicture.setProduct(pictureHostingService);
		uploadingPicture.setLimits(new ArrayList<>());
		repository.addFeature(uploadingPicture);

		// C O U N T L I M I T
		CountLimit maximumPictureSize = new CountLimit("Maximum picture size", 10);
		maximumPictureSize.setUnit("Go");
		uploadingPicture.getLimits().add(maximumPictureSize);

		// R A T E L I M I T
		RateLimit maximumPicturesUploadedByHour = new RateLimit(10, TimeUnit.HOURS, 1);
		maximumPicturesUploadedByHour.setId("Maximum of pictures uploaded by hour");
		uploadingPicture.getLimits().add(maximumPicturesUploadedByHour);

		// F E A T U R E :  D O W N L O A D I N G   P I C T U R E S
		Feature downloadingPicture = new Feature();
		downloadingPicture.setFeatureId("Downloading pictures");
		downloadingPicture.setProduct(pictureHostingService);
		downloadingPicture.setLimits(new ArrayList<>());
		repository.addFeature(downloadingPicture);

		// R A T E L I M I T
		RateLimit maximumPicturesDownloadedByHour = new RateLimit(8, TimeUnit.MINUTES, 60);
		maximumPicturesDownloadedByHour.setId("Maximum of pictures downloaded by hour");
		downloadingPicture.getLimits().add(maximumPicturesDownloadedByHour);


		// P R O D U C T :  L I B R A R Y
		Product lendingBooks = new Product();
		lendingBooks.setProductId("Library");

		// F E A T U R E :  R E S E R V I N G   B O O K S
		Feature reservingBooks = new Feature();
		reservingBooks.setFeatureId("Reserving books");
		reservingBooks.setProduct(lendingBooks);
		reservingBooks.setLimits(new ArrayList<>());
		repository.addFeature(reservingBooks);

		// C O U N T L I M I T
		CountLimit maximumBooksReserved = new CountLimit("Maximum books reserved", 5);
		reservingBooks.getLimits().add(maximumBooksReserved);


		FileOutputStream fos = new FileOutputStream("features_repository.json");
		repository.store(fos);
		fos.close();
	}

	@Test
	@Order(1)
	void load_jsonWithFeatures() throws IOException {

		FileInputStream fis = new FileInputStream("features_repository.json");
		repository.load(fis);
		fis.close();

		// P R O D U C T :  P I C T U R E   H O S T I N G   S E R V I C E
		Product pictureHostingService = new Product();
		pictureHostingService.setProductId("Picture hosting service");

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Optional<Feature> uploadingPicture = repository.getFeature(pictureHostingService, "Uploading pictures");
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
		assertTrue(uploadingPicture.get().getLimits().get(1) instanceof RateLimit);
		RateLimit maximumPicturesInMonth = (RateLimit)uploadingPicture.get().getLimits().get(1);
		assertEquals("Maximum of pictures uploaded by hour", maximumPicturesInMonth.getId());
		assertEquals(10, maximumPicturesInMonth.getValue());
		assertEquals(TimeUnit.HOURS, maximumPicturesInMonth.getInterval());
		assertEquals(1, maximumPicturesInMonth.getDuration());

		// F E A T U R E :  U P L O A D I N G   P I C T U R E S
		Optional<Feature> downloadingPicture = repository.getFeature(pictureHostingService, "Downloading pictures");
		assertEquals("Downloading pictures", downloadingPicture.get().getFeatureId());
		assertEquals("Picture hosting service", downloadingPicture.get().getProduct().getProductId());
		assertEquals(1, downloadingPicture.get().getLimits().size());

		// R A T E L I M I T
		assertTrue(downloadingPicture.get().getLimits().get(0) instanceof RateLimit);
		maximumPicturesInMonth = (RateLimit)downloadingPicture.get().getLimits().get(0);
		assertEquals("Maximum of pictures downloaded by hour", maximumPicturesInMonth.getId());
		assertEquals(8, maximumPicturesInMonth.getValue());
		assertEquals(TimeUnit.MINUTES, maximumPicturesInMonth.getInterval());
		assertEquals(60, maximumPicturesInMonth.getDuration());


		// P R O D U C T :  L I B R A R Y
		Product library = new Product();
		library.setProductId("Library");

		// F E A T U R E :  R E S E R V I N G   B O O K S
		Optional<Feature> reservingBooks = repository.getFeature(library, "Reserving books");
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
