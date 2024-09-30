package io.terpomo.pmitz.limits.userlimit;

import javax.sql.DataSource;

import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserLimitRepositoryTest {

	@Test
	void builderJdbcRepositoryShouldReturnJDBCUserLimitRepository() {
		var dataSource = mock(DataSource.class);

		var jdbcUserLimitRepo = UserLimitRepository.builder().jdbcRepository(dataSource, "schema", "tableName");

		assertThat(jdbcUserLimitRepo).isNotNull()
				.isInstanceOf(JDBCUserLimitRepository.class);
	}

	@Test
	void builderNoOpRepositoryShouldReturnNoOpUserLimitRepository() {
		var noOpUserLimitRepo = UserLimitRepository.builder().noOpRepository();
		assertThat(noOpUserLimitRepo).isNotNull().isInstanceOf(UsageLimitResolverImpl.NoOpUserLimitRepository.class);
	}
}