package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierBuilderTest {

    @Mock
    DataSource dataSource;

    @Mock
    UsageLimitResolver usageLimitResolver;

    @Mock
    UserLimitRepository userLimitRepository;

    @Test
    void builderShouldCreateUsageLimitVerifierWithMinimalConfig() {
        var usageLimitVerifier = UsageLimitVerifierBuilder.of(UsageLimitVerifierBuilder.inMemoryProductRepo())
                .withDefaultUsageLimitResolver()
                .withJdbcUsageRepository(dataSource, "schema", "table")
                .build();
        assertNotNull(usageLimitVerifier);
    }

    @Test
    void builderShouldCreateUsageLimitVerifierWithUserLimitRespository(){
        var usageLimitVerifier = UsageLimitVerifierBuilder.of(UsageLimitVerifierBuilder.inMemoryProductRepo())
                .withDefaultUsageLimitResolver(userLimitRepository)
                .withJdbcUsageRepository(dataSource, "schema", "table")
                .build();
        assertNotNull(usageLimitVerifier);
    }
    @Test
    void builderShouldCreateUsageLimitVerifierWithCustomUsageLimitResolver(){
        var usageLimitVerifier = UsageLimitVerifierBuilder.of(UsageLimitVerifierBuilder.inMemoryProductRepo())
                .withCustomUsageLimitResolver(usageLimitResolver)
                .withJdbcUsageRepository(dataSource, "schema", "table")
                .build();
        assertNotNull(usageLimitVerifier);
    }

    @Test
    void builderShouldCreateUsageLimitVerifierWithCustomUsageRepository(){
        UsageRepository usageRepository = mock(UsageRepository.class);
        var usageLimitVerifier = UsageLimitVerifierBuilder.of(UsageLimitVerifierBuilder.inMemoryProductRepo())
                .withDefaultUsageLimitResolver()
                .withCustomUsageRepository(usageRepository)
                .build();
        assertNotNull(usageLimitVerifier);
    }

    @Test
    void builderShouldCreateUsageLimitVerifierWithCustomVerificationStrategy(){
        var verificationStrategy = mock(UsageLimitVerificationStrategy.class);
        var usageLimitVerifier = UsageLimitVerifierBuilder.of(UsageLimitVerifierBuilder.inMemoryProductRepo())
                .withDefaultUsageLimitResolver()
                .withJdbcUsageRepository(dataSource, "schema", "table")
                .withUserLimitVerificationStrategy(verificationStrategy)
                .build();
        assertNotNull(usageLimitVerifier);
    }
}