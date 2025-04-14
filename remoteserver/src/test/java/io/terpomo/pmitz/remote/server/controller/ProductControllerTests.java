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

package io.terpomo.pmitz.remote.server.controller;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.repository.product.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import(Jackson2ObjectMapperBuilderMixinCustomizer.class)
class ProductControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProductRepository productRepository;

	@Captor
	ArgumentCaptor<Product> productArgumentCaptor;

	@Test
	void addProductShouldAddProductToRepository() throws Exception {
		doNothing().when(productRepository).addProduct(productArgumentCaptor.capture());

		String jsonContent = new ClassPathResource("/product-picshare.json").getContentAsString(StandardCharsets.UTF_8);

		String url = "/products";
		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isOk());

		verify(productRepository).addProduct(productArgumentCaptor.capture());

		var product = productArgumentCaptor.getValue();
		assertThat(product).isNotNull();
		assertThat(product.getProductId()).isEqualTo("picshare");

		assertThat(product.getFeatures()).hasSize(2);
		var featureOptional = product.getFeatures().stream().filter(feature -> "Downloading pictures".equals(feature.getFeatureId())).findFirst();
		assertThat(featureOptional).isNotEmpty();

		var limits = featureOptional.get().getLimits();
		var limitOptional = limits.stream().filter(limit -> limit.getId().equals("Maximum of pictures downloaded by calendar month")).findFirst();
		assertThat(limitOptional).isNotEmpty();

		var limit = limitOptional.get();
		assertThat(limit).isInstanceOf(CalendarPeriodRateLimit.class);
		assertThat(((CalendarPeriodRateLimit) limit).getPeriodicity()).isEqualTo(CalendarPeriodRateLimit.Periodicity.MONTH);
		assertThat(limit.getValue()).isEqualTo(10);
	}

	@Test
	void addProductWhenAlreadyExistsShouldReturnStatus409() throws Exception {
		when(productRepository.getProductById("picshare")).thenReturn(Optional.of(new Product("picshare")));
		String jsonContent = new ClassPathResource("/product-picshare.json").getContentAsString(StandardCharsets.UTF_8);

		String url = "/products";
		mockMvc.perform(post(url)
						.contentType("application/json")
						.content(jsonContent))
				.andExpect(status().isConflict());

		verify(productRepository, never()).addProduct(any());
	}

	@Test
	void removeProductShouldRemoveProductFromRepository() throws Exception {
		when(productRepository.getProductById("picshare")).thenReturn(Optional.of(new Product("picshare")));
		doNothing().when(productRepository).removeProduct(productArgumentCaptor.capture());

		String url = "/products/picshare";
		mockMvc.perform(delete(url)
						.contentType("application/json"))
				.andExpect(status().isOk());

		verify(productRepository).removeProduct(productArgumentCaptor.capture());

		var product = productArgumentCaptor.getValue();
		assertThat(product).isNotNull();
		assertThat(product.getProductId()).isEqualTo("picshare");
	}

	@Test
	void removeProductWhenDoesNotExistShouldReturnStatus404() throws Exception {
		when(productRepository.getProductById("aProductId")).thenReturn(Optional.empty());

		String url = "/products/aProductId";
		mockMvc.perform(delete(url)
						.contentType("application/json"))
				.andExpect(status().isNotFound());

		verify(productRepository, never()).removeProduct(any());
	}
}
