package com.bankcore.account.integration;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.repository.AccountRepository;
import com.bankcore.common.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("account_service_test")
            .withUsername("bankcore")
            .withPassword("bankcore");

    @Autowired
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void savesAndLoadsAccountCreatedByDomainFactory() {
        UUID userId = UUID.randomUUID();
        Account account = Account.open(userId, "BC12345678901234", AccountType.CHECKING, Currency.EUR);

        Account saved = accountRepository.saveAndFlush(account);

        assertThat(accountRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(loaded -> {
                    assertThat(loaded.getUserId()).isEqualTo(userId);
                    assertThat(loaded.getAccountNumber()).isEqualTo("BC12345678901234");
                    assertThat(loaded.getType()).isEqualTo(AccountType.CHECKING);
                    assertThat(loaded.getCurrency()).isEqualTo(Currency.EUR);
                    assertThat(loaded.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
                    assertThat(loaded.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
                    assertThat(loaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
                    assertThat(loaded.getVersion()).isNotNull();
                    assertThat(loaded.getCreatedAt()).isNotNull();
                    assertThat(loaded.getUpdatedAt()).isNotNull();
                });
    }
}
