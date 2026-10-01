package com.banking.account.repository;

import com.banking.account.domain.Account;
import com.banking.account.domain.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Acquires a pessimistic write lock for high-concurrency debit/credit operations
     * preventing race conditions between concurrent channel operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /**
     * Utilizes composite index idx_accounts_customer_status (customer_id, status)
     */
    List<Account> findByCustomerIdAndStatus(String customerId, AccountStatus status);

    List<Account> findByCustomerId(String customerId);

    /**
     * High-performance query utilizing composite index idx_accounts_curr_bal (currency, available_balance)
     */
    @Query("SELECT a FROM Account a WHERE a.currency = :currency AND a.availableBalance >= :minBalance ORDER BY a.availableBalance DESC")
    Page<Account> findByCurrencyAndMinimumBalance(@Param("currency") String currency,
                                                  @Param("minBalance") BigDecimal minBalance,
                                                  Pageable pageable);
}
