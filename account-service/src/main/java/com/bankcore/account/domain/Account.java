package com.bankcore.account.domain;

import com.bankcore.common.util.Currency;
import com.bankcore.common.util.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing a bank account.
 *
 * <h2>Concurrency strategy</h2>
 * <ul>
 *   <li><strong>Optimistic locking</strong> ({@link #version}) — used for low-contention
 *       updates such as status changes. JPA increments {@code version} on every flush;
 *       a stale-state write throws {@code OptimisticLockingFailureException}, which
 *       {@code AccountService} retries via {@code @Retryable}.</li>
 *   <li><strong>Pessimistic locking</strong> — balance mutations ({@code debit},
 *       {@code credit}) use {@code SELECT … FOR UPDATE} via
 *       {@code AccountRepository#findByIdForUpdate}. This serialises concurrent balance
 *       writes at the DB level, making the application-level balance check safe.</li>
 * </ul>
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false,
            length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {}

    public static Account open(UUID userId, String accountNumber, AccountType type,
                               Currency currency) {
        Account account = new Account();
        account.id = IdGenerator.generate();
        account.userId = userId;
        account.accountNumber = accountNumber;
        account.type = type;
        account.currency = currency;
        account.balance = BigDecimal.ZERO.setScale(2);
        account.availableBalance = BigDecimal.ZERO.setScale(2);
        account.status = AccountStatus.ACTIVE;
        return account;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }

    public String getAccountNumber() { return accountNumber; }

    public AccountType getType() { return type; }

    public Currency getCurrency() { return currency; }

    public BigDecimal getBalance() { return balance; }

    public BigDecimal getAvailableBalance() { return availableBalance; }

    public AccountStatus getStatus() { return status; }

    public Long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.availableBalance = balance;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Account{"
                + "id=" + id
                + ", accountNumber='" + accountNumber + '\''
                + ", type=" + type
                + ", currency=" + currency
                + ", status=" + status
                + ", version=" + version
                + '}';
    }
}