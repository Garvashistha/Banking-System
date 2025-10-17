package org.bank.service;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.entities.Transaction;
import org.bank.repository.AccountRepository;
import org.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TransactionService with concurrency control and BigDecimal-safe arithmetic.
 * Notes:
 * - Uses optimistic locking on Account (requires @Version in Account entity).
 * - Updates Account balance inside a @Transactional method and saves the Transaction.
 * - Retries on optimistic lock conflicts up to MAX_RETRIES.
 * - Preserves original public method signatures and find methods.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_RETRIES = 3;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * Save a transaction safely with concurrency control.
     *
     * - If transaction.account is present and accountId is non-null, the service will:
     *   * fetch the latest Account,
     *   * update the Account.balance according to transactionType (deposit/withdraw/transfer),
     *   * save the Account and then save the Transaction, all inside a transaction.
     *
     * - Uses BigDecimal arithmetic to avoid precision errors and compilation issues.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction save(Transaction transaction) {
        int attempts = 0;

        while (true) {
            try {
                // Ensure timestamp
                if (transaction.getTimestamp() == null) {
                    transaction.setTimestamp(LocalDateTime.now());
                }

                // If transaction references an account, update balance accordingly
                if (transaction.getAccount() != null && transaction.getAccount().getAccountId() != null) {
                    Long accountId = transaction.getAccount().getAccountId();

                    Account account = accountRepository.findById(accountId)
                            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                    // Normalize transaction amount to BigDecimal
                    BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;

                    String type = transaction.getTransactionType() != null
                            ? transaction.getTransactionType().toLowerCase().trim()
                            : "";

                    // Current balance (non-null expected; if null treat as zero)
                    BigDecimal currentBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

                    switch (type) {
                        case "deposit":
                            account.setBalance(currentBalance.add(amount));
                            break;

                        case "withdraw":
                            // Reject if insufficient funds
                            if (currentBalance.compareTo(amount) < 0) {
                                throw new IllegalStateException("Insufficient balance for withdrawal");
                            }
                            account.setBalance(currentBalance.subtract(amount));
                            break;

                        case "transfer":
                            // Here we treat a 'transfer' on this transaction as a debit from this account.
                            // If your app creates a separate credit transaction for destination account,
                            // that should be handled by the code that calls this service.
                            if (currentBalance.compareTo(amount) < 0) {
                                throw new IllegalStateException("Insufficient balance for transfer");
                            }
                            account.setBalance(currentBalance.subtract(amount));
                            break;

                        default:
                            // Unknown transaction type: do not modify account balance.
                            break;
                    }

                    // Save account (may throw OptimisticLockingFailureException if version mismatch)
                    accountRepository.save(account);

                    // Make sure the transaction references the managed account instance
                    transaction.setAccount(account);
                }

                // Save transaction record
                return transactionRepository.save(transaction);
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw ex;
                }
                // small backoff before retrying
                try {
                    Thread.sleep(100L * attempts);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                // retry loop continues
            }
        }
    }

    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findBySourceAccountId(Long accountId) {
        return transactionRepository.findBySourceAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findByDestinationAccountId(Long accountId) {
        return transactionRepository.findByDestinationAccountAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> findByCustomer(Customer customer) {
        return transactionRepository.findByAccountCustomerOrderByTimestampDesc(customer);
    }
}
