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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.repository.product.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {

	private final Map<String, Product> products = new ConcurrentHashMap<>();

	@Override
	public List<String> getProductIds() {

		return products.keySet().stream().toList();
	}

	@Override
	public Optional<Product> getProductById(String productId) {

		if (productId == null) {
			throw new RepositoryException(("ProductId must not be 'null'"));
		}

		return Optional.ofNullable(products.get(productId));
	}

	@Override
	public void addProduct(Product product) {

		validateProduct(product);

		if (products.containsKey(product.getProductId())) {
			throw new RepositoryException((String.format("Product '%s' already exist", product.getProductId())));
		}

		products.put(product.getProductId(), product);
	}

	@Override
	public void updateProduct(Product product) {

		validateProduct(product);

		if (!products.containsKey(product.getProductId())) {
			throw new RepositoryException((String.format("Product '%s' not found", product.getProductId())));
		}

		products.put(product.getProductId(), product);
	}

	@Override
	public void removeProduct(Product product) {

		validateProduct(product);

		if (!products.containsKey(product.getProductId())) {
			throw new RepositoryException((String.format("Product '%s' not found", product.getProductId())));
		}

		products.remove(product.getProductId());
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
}
