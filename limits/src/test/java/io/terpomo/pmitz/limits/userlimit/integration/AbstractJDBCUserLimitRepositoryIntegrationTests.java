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

package io.terpomo.pmitz.limits.userlimit.integration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractJDBCUserLimitRepositoryIntegrationTests {

	protected static final String CUSTOM_SCHEMA = "pmitz";
	protected static final String TABLE_NAME = "user_usage_limit";

	protected final Product product = new Product("Picture hosting service");
	protected final Feature feature = new Feature(this.product, "Uploading pictures");
	protected final UserGrouping user = new IndividualUser("User1");
	protected BasicDataSource dataSource;
	protected JDBCUserLimitRepository repository;

	protected abstract void setupDataSource() throws SQLException;

	protected abstract void setupDatabase() throws SQLException, IOException;

	protected abstract void tearDownDatabase() throws SQLException, IOException;

	protected abstract void populateTable() throws SQLException;

	@BeforeEach
	void setUp() throws SQLException, IOException {
		setupDataSource();
		setupDatabase();
		populateTable();
	}

	@AfterEach
	void tearDown() throws SQLException, IOException {
		tearDownDatabase();
	}

	@Test
	public void testUpdateLimitRule() {

		CountLimit countLimitToModified = new CountLimit("Maximum number of picture", 15);

		this.repository.updateLimitRule(this.feature, countLimitToModified, this.user);

		Optional<LimitRule> limitRule = this.repository.findLimitRule(this.feature,
				"Maximum number of picture", this.user);

		assertThat(limitRule).isPresent();
		assertThat(limitRule.get()).isInstanceOf(CountLimit.class)
				.extracting(LimitRule::getId).isEqualTo("Maximum number of picture");
		assertThat(limitRule.get()).isInstanceOf(CountLimit.class)
				.extracting(LimitRule::getValue).isEqualTo(15L);
	}
}
