package io.terpomo.pmitz;

import java.io.IOException;
import java.util.Collections;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.limits.UsageLimitVerifierBuilder;
import org.h2.jdbcx.JdbcDataSource;

public class InMemorySample {

	private static final String USER_USAGE_TABLE_NAME = "usage";
	private static final String DB_SCHEMA_NAME = "dbo";

	private UsageLimitVerifier usageLimitVerifier;
	private Product product;

	public static void main(String[] args) {
		JdbcDataSource dataSource;
		try {
			Class.forName ("org.h2.Driver");

			dataSource = new JdbcDataSource();
			dataSource.setURL("jdbc:h2:mem:dbo;INIT=RUNSCRIPT FROM 'classpath:/init-usage-repo.sql';");
			dataSource.setUser("sa");
			dataSource.setPassword("");


		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		InMemorySample sampleApp = new InMemorySample();

		sampleApp.initLimitVerifier(dataSource);

		// First call : Within the limit
		sampleApp.reserveBooks(5);

		// Second call : Fail because the limit reached in the first call
		sampleApp.reserveBooks(1);

	}

	public void reserveBooks(int numberOfBooks){
		Feature feature = product.getFeatures().stream().filter(ft -> ft.getFeatureId().equals("Reserving books")).findFirst().get();

		try {
			usageLimitVerifier.recordFeatureUsage(feature, new IndividualUser("user001"), Collections.singletonMap("Maximum books reserved", (long)numberOfBooks));

			// Your business logic here
			System.out.println("Books reserved!");

		} catch (LimitExceededException e){
			System.out.println("Oops! Looks like you exceeded your reservation limit");
		}

	}
	private void initLimitVerifier(DataSource usageRepoDataSource){
		InMemoryProductRepository productRepo = new InMemoryProductRepository();
		try {
			productRepo.load(InMemorySample.class.getResourceAsStream("/products_repository.json"));
		}
		catch (IOException e) {
			throw new RuntimeException("Product Repository file not found", e);
		}

		var optProduct = productRepo.getProductById("Library");
		if (optProduct.isEmpty()){
			throw new IllegalStateException("Product not found in repo");
		}
		product = optProduct.get();

		usageLimitVerifier = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver()
				.withJdbcUsageRepository(usageRepoDataSource, DB_SCHEMA_NAME, USER_USAGE_TABLE_NAME)
				.build();
	}


}
