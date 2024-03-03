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

package io.terpomo.pmitz.core.integration;

import java.sql.SQLException;
import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.repository.userlimit.jdbc.JDBCUserLimitRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractJDBCUserLimitRepositoryIntegrationTests {

	protected static final String SCHEMA_NAME = "public";
	protected static final String TABLE_NAME = "user_usage_limit";
	protected final Product product = new Product("Picture hosting service");
	protected final Feature feature = new Feature(this.product, "Uploading pictures");
	protected final UserGrouping user = new IndividualUser("User1");
	protected BasicDataSource dataSource;
	protected JDBCUserLimitRepository repository;

	protected abstract void setupDataSource() throws SQLException;

	protected abstract void setupDatabase() throws SQLException;

	protected abstract void populateTable() throws SQLException;

	protected abstract void tearDownDatabase() throws SQLException;

	// TODO: Remove
	protected abstract void printDatabaseContents();

	@BeforeEach
	void setUp() throws SQLException {
		setupDataSource();
		setupDatabase();
		populateTable();
	}

	@AfterEach
	void tearDown() throws Exception {
		tearDownDatabase();
	}

	@Test
	public void testUpdateUserLimit() throws SQLException {

		printDatabaseContents();

		CountLimit countLimitToModified = new CountLimit("Maximum number of picture", 15);

		this.repository.updateUsageLimit(this.feature, countLimitToModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture", this.user);

		assertTrue(userLimit.isPresent());
		CountLimit countLimitDb = assertInstanceOf(CountLimit.class, userLimit.get());
		assertEquals("Maximum number of picture", countLimitDb.getId());
		assertEquals(15, countLimitDb.getValue());

		printDatabaseContents();
	}
}
