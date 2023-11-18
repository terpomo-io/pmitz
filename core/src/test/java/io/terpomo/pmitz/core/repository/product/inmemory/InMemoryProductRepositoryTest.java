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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import static org.junit.jupiter.api.Assertions.*;

public class InMemoryProductRepositoryTest {

	@Test
	void getProductIds_emptyList() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertTrue(ids.isEmpty());
	}

	@Test
	void getProductIds_1ItemList() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertEquals(1, ids.size());
	}

	@Test
	void getProductIds_multipleItemList() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Product p2 = new Product();
		p2.setProductId("p2");
		Product p3 = new Product();
		p3.setProductId("p3");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);
		repository.addProduct(p2);
		repository.addProduct(p3);

		List<String> ids = repository.getProductIds();

		assertNotNull(ids);
		assertEquals(3, ids.size());
	}

	@Test
	void getProductById_productIdNull() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.getProductById(null);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void getProductById_existingProduct() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());
	}

	@Test
	void getProductById_notExistingProduct() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isEmpty());
	}

	@Test
	void addProduct_productNull() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void addProduct_productWithIdNull() {

		Product p1 = new Product();
		p1.setProductId(null);

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void addProduct_newProduct() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());
	}

	@Test
	void addProduct_existingProduct() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1, p.get());

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.addProduct(p1);
		});

		assertEquals("Product 'p1' already exist", exception.getMessage());
	}

	@Test
	void updateProduct_productNull() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateProduct(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void updateProduct_productWithIdNull() {

		Product p1 = new Product();
		p1.setProductId(null);

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateProduct(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void updateProduct_existingProduct() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Product p2 = new Product();
		p2.setProductId("p2");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);
		repository.addProduct(p2);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());

		p = repository.getProductById("p2");
		assertTrue(p.isPresent());
		assertEquals(p2.getProductId(), p.get().getProductId());

		repository.updateProduct(p2);

		p = repository.getProductById("p2");
		assertTrue(p.isPresent());
		assertEquals(p2.getProductId(), p.get().getProductId());
	}

	@Test
	void updateProduct_productNotFound() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1, p.get());

		Product p2 = new Product();
		p2.setProductId("p2");

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.updateProduct(p2);
		});

		assertEquals("Product 'p2' not found", exception.getMessage());
	}

	@Test
	void removeProduct_productNull() {

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(null);
		});

		assertEquals("Product must not be 'null'", exception.getMessage());
	}

	@Test
	void removeProduct_productWithIdNull() {

		Product p1 = new Product();
		p1.setProductId(null);

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(p1);
		});

		assertEquals("ProductId must not be 'null'", exception.getMessage());
	}

	@Test
	void removeProduct_existingProduct() {

		Product p1 = new Product();
		p1.setProductId("p1");
		Product p2 = new Product();
		p2.setProductId("p2");

		InMemoryProductRepository repository = new InMemoryProductRepository();
		repository.addProduct(p1);
		repository.addProduct(p2);

		Optional<Product> p = repository.getProductById("p1");
		assertTrue(p.isPresent());
		assertEquals(p1.getProductId(), p.get().getProductId());

		p = repository.getProductById("p2");
		assertTrue(p.isPresent());
		assertEquals(p2.getProductId(), p.get().getProductId());

		repository.removeProduct(p2);

		p = repository.getProductById("p2");
		assertTrue(p.isEmpty());
	}

	@Test
	void removeProduct_productNotFound() {

		Product p1 = new Product();
		p1.setProductId("p1");

		InMemoryProductRepository repository = new InMemoryProductRepository();

		RepositoryException exception = assertThrows(RepositoryException.class, () -> {
			repository.removeProduct(p1);
		});

		assertEquals("Product 'p1' not found", exception.getMessage());
	}
}
