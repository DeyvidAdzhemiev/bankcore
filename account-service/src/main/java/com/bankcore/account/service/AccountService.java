package com.bankcore.account.service;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.api.dto.CreateAccountRequest;
import com.bankcore.account.api.mapper.AccountMapper;
import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.repository.AccountRepository;
import com.bankcore.common.exception.AccountNotFoundException;
import com.bankcore.common.exception.InsufficientFundsException;
import com.bankcore.common.util.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core business logic for account management.
 *
 * <h2>Transaction strategy</h2>
 * <ul>
 *   <li>Class-level {@code @Transactional} sets {@code READ_COMMITTED, readOnly=true}
 *       as the default for all methods (safe reads, no accidental writes).
 *   <li>Write methods ({@code createAccount}, {@code debit}, {@code credit},
 *       {@code updateStatus}) override with {@code readOnly=false}. The isolation level
 *       remains {@code READ_COMMITTED}; concurrency safety is provided by locking, not
 *       by a higher isolation level (which would cause more lock contention).</li>
 * </ul>
 *
 * <h2>Cache strategy</h2>
 * <p>{@link #getAccount} is annotated {@code @Cacheable}; write methods that change an
 * account are annotated {@code @CacheEvict}. The {@code RedisCacheManager} is configured
 * with {@code transactionAware = true} (see {@code CacheConfig}), meaning cache writes
 * and evictions are only applied after the surrounding transaction commits — preventing
 * stale data from entering the cache when a transaction rolls back.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private static final String CACHE_NAME = "accounts";
    private static final int MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Cacheable(value = CACHE_NAME, key = "#id.toString()")
    public AccountResponse getAccount(UUID id) {
        log.debug("Cache miss for account {}, loading from database", id);
        return accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<AccountResponse> getAccountsByUser(UUID userId) {
        return accountMapper.toResponseList(accountRepository.findByUserId(userId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = false)
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNumber = generateUniqueAccountNumber();
        Account account = Account.open(
                request.userId(),
                accountNumber,
                request.type(),
                request.currency()
        );
        Account saved = accountRepository.save(account);
        log.info("Opened account {} ({} {}) for user {}",
                saved.getAccountNumber(), saved.getCurrency(), saved.getType(), saved.getUserId());
        return accountMapper.toResponse(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = false)
    @CacheEvict(value = CACHE_NAME, key = "#accountId.toString()")
    public AccountResponse debit(UUID accountId, Money amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        requireActive(account);
        requireSameCurrency(account, amount);

        Money currentBalance = Money.of(account.getAvailableBalance(), account.getCurrency());
        if (!currentBalance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException(accountId,
                    amount.toString(), currentBalance.toString());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount.amount());
        account.setBalance(newBalance);

        log.info("Debited {} from account {} (new balance: {} {})",
                amount, account.getAccountNumber(),
                newBalance.toPlainString(), account.getCurrency());

        return accountMapper.toResponse(account);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = false)
    @CacheEvict(value = CACHE_NAME, key = "#accountId.toString()")
    public AccountResponse credit(UUID accountId, Money amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        requireActive(account);
        requireSameCurrency(account, amount);

        BigDecimal newBalance = account.getBalance().add(amount.amount());
        account.setBalance(newBalance);

        log.info("Credited {} to account {} (new balance: {} {})",
                amount, account.getAccountNumber(),
                newBalance.toPlainString(), account.getCurrency());

        return accountMapper.toResponse(account);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = false)
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @CacheEvict(value = CACHE_NAME, key = "#accountId.toString()")
    public AccountResponse updateStatus(UUID accountId, AccountStatus newStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        AccountStatus previous = account.getStatus();
        requireValidStatusTransition(previous, newStatus);
        account.setStatus(newStatus);

        log.info("Account {} status changed: {} → {}", account.getAccountNumber(),
                previous, newStatus);
        return accountMapper.toResponse(account);
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 1; attempt <= MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            long randomPart = ThreadLocalRandom.current().nextLong(0L, 99_999_999_999_999L);
            String candidate = String.format("BC%014d", randomPart);
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Unable to generate unique account number after "
                        + MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS + " attempts");
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Account " + account.getId() + " is " + account.getStatus()
                            + " and does not accept transactions");
        }
    }

    private void requireSameCurrency(Account account, Money amount) {
        if (!account.getCurrency().equals(amount.currency())) {
            throw new IllegalArgumentException(
                    "Currency mismatch: account is " + account.getCurrency()
                            + " but operation amount is in " + amount.currency());
        }
    }

    private void requireValidStatusTransition(AccountStatus currentStatus, AccountStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean valid = switch (currentStatus) {
            case ACTIVE -> newStatus == AccountStatus.FROZEN || newStatus == AccountStatus.CLOSED;
            case FROZEN -> newStatus == AccountStatus.ACTIVE || newStatus == AccountStatus.CLOSED;
            case CLOSED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid account status transition: " + currentStatus + " -> " + newStatus);
        }
    }
}
