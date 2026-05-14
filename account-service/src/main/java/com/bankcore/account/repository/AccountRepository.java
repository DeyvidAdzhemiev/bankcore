package com.bankcore.account.repository;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Account} persistence.
 *
 * <h2>Locking</h2>
 * <ul>
 *   <li>{@link #findByIdForUpdate} — acquires a {@code PESSIMISTIC_WRITE} (SELECT FOR UPDATE)
 *       lock. Concurrent callers will block until the lock is released (i.e. the transaction commits
 *       or rolls back). This prevents over-debit races.</li>
 *   <li>All other finders use the default read access. Optimistic locking (via
 *       {@code @Version} on the entity) protects low-contention writes such as status
 *       updates without ever blocking readers.</li>
 * </ul>
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    List<Account> findByUserIdAndStatus(UUID userId, AccountStatus status);

    List<Account> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);
}