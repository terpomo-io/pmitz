package io.terpomo.pmitz.limits.userlimit;

import javax.sql.DataSource;

import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;

public class DefaultUserLimitRepositoryBuilder implements UserLimitRepository.Builder {
	@Override
	public UserLimitRepository jdbcRepository(DataSource dataSource, String dbSchema, String tableName) {
		return new JDBCUserLimitRepository(dataSource, dbSchema, tableName);
	}

	@Override
	public UserLimitRepository noOpRepository() {
		return new UsageLimitResolverImpl.NoOpUserLimitRepository();
	}
}
