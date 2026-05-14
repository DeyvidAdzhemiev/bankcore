package com.bankcore.account.config;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.common.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void redisCacheValueSerializerWritesAccountResponseWithInstants() {
        RedisCacheConfiguration configuration = new CacheConfig().defaultRedisCacheConfiguration();
        AccountResponse response = new AccountResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BC12345678901234",
                AccountType.CHECKING,
                Currency.EUR,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                AccountStatus.ACTIVE,
                0L,
                Instant.parse("2026-05-14T14:00:00Z"),
                Instant.parse("2026-05-14T14:00:01Z")
        );

        assertThatCode(() -> configuration.getValueSerializationPair().write(response))
                .doesNotThrowAnyException();

        ByteBuffer serialized = configuration.getValueSerializationPair().write(response);

        assertThat(configuration.getValueSerializationPair().read(serialized))
                .isInstanceOf(AccountResponse.class)
                .usingRecursiveComparison()
                .isEqualTo(response);
    }
}
