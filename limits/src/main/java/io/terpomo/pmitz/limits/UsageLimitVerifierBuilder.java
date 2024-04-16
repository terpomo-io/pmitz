package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.exception.ConfigurationException;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.impl.UsageLimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.UsageLimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

public class UsageLimitVerifierBuilder {

    private UsageLimitResolver usageLimitResolver;
    private ProductRepository productRepository;
    private UserLimitRepository userLimitRepository;
    private UsageRepository usageRepository;
    private UsageLimitVerificationStrategyResolver usageLimitVerificationStrategyResolver;


    private UsageLimitVerifierBuilder (){
        // private constructor
    }

    public static UsageLimitVerifierBuilder of(ProductRepository productRepository){

        var builder = new UsageLimitVerifierBuilder();
        builder.productRepository = productRepository;
        return builder;
    }

    public UsageLimitVerifierBuilder withUsageLimitResolver (UsageLimitResolver usageLimitResolver){
        this.usageLimitResolver = usageLimitResolver;
        return this;
    }

    public UsageLimitVerifierBuilder withUserLimitRepository (UserLimitRepository userLimitRepository){
        this.userLimitRepository = userLimitRepository;
        return this;
    }

    public UsageLimitVerifierBuilder withUsageLimitVerificationStrategyResolver (UsageLimitVerificationStrategyResolver strategyResolver){
        this.usageLimitVerificationStrategyResolver = strategyResolver;
        return this;
    }

    public UsageLimitVerifierBuilder withUsageRepository (UsageRepository usageRepository){
        this.usageRepository = usageRepository;
        return this;
    }

    public UsageLimitVerifier build() {

//       ProductRepository productRepo = null;

//        var limitVerifBuilder = UsageLimitVerifierBuilder.of(productRepository)
//                .withUserLimitRepository(new JDBCUserLimitRepository(null, null, null));
//

        var verificationStrategyResolver = usageLimitVerificationStrategyResolver != null
                ? usageLimitVerificationStrategyResolver : new UsageLimitVerificationStrategyDefaultResolver();

        return new UsageLimitVerifierImpl(getUsageLimitResolver(),
                verificationStrategyResolver,
                usageRepository);
    }

    private UsageLimitResolver getUsageLimitResolver(){
        if (usageLimitResolver != null){
            return usageLimitResolver;
        } else if (userLimitRepository == null){
            throw new ConfigurationException("Either UsageLimitResolver or UserLimitRepository is needed");
        }
        return new UsageLimitResolverImpl(productRepository, userLimitRepository);
    }
}
