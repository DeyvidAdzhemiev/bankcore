package com.bankcore.account.integration;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.api.mapper.AccountMapper;
import com.bankcore.account.config.CacheConfig;
import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.repository.AccountRepository;
import com.bankcore.account.service.AccountService;
import com.bankcore.common.util.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AccountCacheRedisIT.TestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class AccountCacheRedisIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("test.redis.host", REDIS::getHost);
        registry.add("test.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test
    void getAccountCachesResponseInRedis() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open(
                UUID.randomUUID(),
                "BC12345678901234",
                AccountType.CHECKING,
                Currency.USD);
        AccountResponse response = new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getType(),
                account.getCurrency(),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                AccountStatus.ACTIVE,
                0L,
                Instant.parse("2026-05-14T14:00:00Z"),
                Instant.parse("2026-05-14T14:00:01Z"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse first = accountService.getAccount(accountId);
        AccountResponse second = accountService.getAccount(accountId);

        assertThat(second).isEqualTo(first);
        verify(accountRepository, times(1)).findById(accountId);
    }

    @AfterEach
    void closeRedisConnectionFactory() {
        if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            lettuceConnectionFactory.destroy();
        }
    }

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory(
                @Value("${test.redis.host}") String host,
                @Value("${test.redis.port}") int port) {
            return new LettuceConnectionFactory(host, port);
        }

        @Bean
        CacheManager cacheManager(
                RedisConnectionFactory redisConnectionFactory,
                RedisCacheConfiguration redisCacheConfiguration) {
            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(redisCacheConfiguration)
                    .transactionAware()
                    .build();
        }

        @Bean
        AccountRepository accountRepository() {
            return mock(AccountRepository.class);
        }

        @Bean
        AccountMapper accountMapper() {
            return mock(AccountMapper.class);
        }

        @Bean
        AccountService accountService(AccountRepository accountRepository, AccountMapper accountMapper) {
            return new AccountService(accountRepository, accountMapper);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition)
                        throws TransactionException {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) throws TransactionException {
                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {
                }
            };
        }
    }
}
