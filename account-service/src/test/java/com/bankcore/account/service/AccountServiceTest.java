package com.bankcore.account.service;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.api.dto.CreateAccountRequest;
import com.bankcore.account.api.mapper.AccountMapper;
import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.repository.AccountRepository;
import com.bankcore.common.exception.InsufficientFundsException;
import com.bankcore.common.util.Currency;
import com.bankcore.common.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

class AccountServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountMapper accountMapper = new TestAccountMapper();
    private final AccountService accountService = new AccountService(accountRepository, accountMapper);

    @Test
    void creditAddsAmountToActiveAccountBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setBalance(new BigDecimal("25.00"));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.credit(accountId, Money.of("40.00", Currency.USD));

        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("65.00"));
        assertThat(response.availableBalance()).isEqualByComparingTo(new BigDecimal("65.00"));
        verify(accountRepository).findByIdForUpdate(accountId);
    }

    @Test
    void debitSubtractsAmountFromActiveAccountBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.debit(accountId, Money.of("35.50", Currency.USD));

        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("64.50"));
        assertThat(response.availableBalance()).isEqualByComparingTo(new BigDecimal("64.50"));
        verify(accountRepository).findByIdForUpdate(accountId);
    }

    @Test
    void debitRejectsInsufficientFunds() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(accountId, Money.of("10.01", Currency.USD)))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("requested 10.01 USD")
                .hasMessageContaining("available 10.00 USD");
    }

    @Test
    void debitRejectsFrozenAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setBalance(new BigDecimal("100.00"));
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(accountId, Money.of("1.00", Currency.USD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FROZEN");
    }

    @Test
    void creditRejectsClosedAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.credit(accountId, Money.of("1.00", Currency.USD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void updateStatusAllowsActiveToFrozen() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.updateStatus(accountId, AccountStatus.FROZEN);

        assertThat(response.status()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void updateStatusRejectsClosedToActive() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateStatus(accountId, AccountStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void updateStatusAllowsFrozenToClosed() {
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount();
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.updateStatus(accountId, AccountStatus.CLOSED);

        assertThat(response.status()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void createAccountFailsAfterBoundedAccountNumberCollisions() {
        when(accountRepository.existsByAccountNumber(any())).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(
                new CreateAccountRequest(UUID.randomUUID(), AccountType.CHECKING, Currency.USD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to generate unique account number");
    }

    @Test
    void createAccountReturnsSavedAccountWhenNumberIsUnique() {
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.createAccount(
                new CreateAccountRequest(UUID.randomUUID(), AccountType.CHECKING, Currency.USD));

        assertThat(response.accountNumber()).startsWith("BC");
        assertThat(response.accountNumber()).hasSize(16);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    private static Account activeAccount() {
        return Account.open(UUID.randomUUID(), "BC12345678901234", AccountType.CHECKING, Currency.USD);
    }

    private static class TestAccountMapper implements AccountMapper {

        @Override
        public AccountResponse toResponse(Account account) {
            return new AccountResponse(
                    account.getId(),
                    account.getUserId(),
                    account.getAccountNumber(),
                    account.getType(),
                    account.getCurrency(),
                    account.getBalance(),
                    account.getAvailableBalance(),
                    account.getStatus(),
                    account.getVersion(),
                    account.getCreatedAt(),
                    account.getUpdatedAt()
            );
        }

        @Override
        public List<AccountResponse> toResponseList(List<Account> accounts) {
            return accounts.stream().map(this::toResponse).toList();
        }
    }
}
