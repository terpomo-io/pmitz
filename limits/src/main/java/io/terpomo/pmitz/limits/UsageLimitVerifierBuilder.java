package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.impl.UsageLimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.UsageLimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

import javax.sql.DataSource;

public class UsageLimitVerifierBuilder {

    private UsageLimitVerifierBuilder (){
        // private constructor
    }

    public static Builder of (ProductRepository productRepository){
        return new Builder(productRepository);
    }

    public interface UsageLimitResolverSpec {
        UsageRepositorySpec withCustomUsageLimitResolver(UsageLimitResolver usageLimitResolver);

        UsageRepositorySpec withDefaultUsageLimitResolver(UserLimitRepository userLimitRepository);

        UsageRepositorySpec withDefaultUsageLimitResolver();
    }

    public interface UsageRepositorySpec {
        LimitVerificationStrategySpec withCustomUsageRepository(UsageRepository usageRepository);

        LimitVerificationStrategySpec withJdbcUsageRepository(DataSource dataSource, String schema, String table);
    }

    public interface LimitVerificationStrategySpec {
       Creator withUserLimitVerificationStrategy (UsageLimitVerificationStrategy stratey);

       UsageLimitVerifier build();
    }

    public interface Creator extends LimitVerificationStrategySpec {
        UsageLimitVerifier build();
    }

    static ProductRepository inMemoryProductRepo(){
        return new InMemoryProductRepository();
    }

    public static class Builder implements UsageLimitResolverSpec, UsageRepositorySpec,
            LimitVerificationStrategySpec, Creator {

        private final ProductRepository productRepository;
        private UsageLimitResolver usageLimitResolver;
        private UsageRepository usageRepository;

        private UsageLimitVerificationStrategyResolver verificationStrategyResolver;

        private Builder(ProductRepository productRepository){
            this.productRepository = productRepository;
        }

        @Override
        public UsageRepositorySpec withCustomUsageLimitResolver(UsageLimitResolver usageLimitResolver) {
            this.usageLimitResolver = usageLimitResolver;
            return this;
        }

        @Override
        public UsageRepositorySpec withDefaultUsageLimitResolver(UserLimitRepository userLimitRepository) {
            usageLimitResolver = new UsageLimitResolverImpl(productRepository, userLimitRepository);
            return this;
        }

        @Override
        public UsageRepositorySpec withDefaultUsageLimitResolver() {
            usageLimitResolver = new UsageLimitResolverImpl(productRepository);
            return this;
        }

        @Override
        public LimitVerificationStrategySpec withCustomUsageRepository(UsageRepository usageRepository) {
            this.usageRepository = usageRepository;
            return this;
        }

        @Override
        public LimitVerificationStrategySpec withJdbcUsageRepository(DataSource dataSource, String schema, String table) {
            usageRepository = new JDBCUsageRepository(dataSource, schema, table);
            return this;
        }

        @Override
        public Creator withUserLimitVerificationStrategy(UsageLimitVerificationStrategy strategy) {
            verificationStrategyResolver = new UsageLimitVerificationStrategyDefaultResolver(strategy);
            return this;
        }

        @Override
        public UsageLimitVerifier build() {
            if (verificationStrategyResolver == null){
                verificationStrategyResolver = new UsageLimitVerificationStrategyDefaultResolver();
            }
            return new UsageLimitVerifierImpl(usageLimitResolver, verificationStrategyResolver, usageRepository);
        }
    }
}


