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

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BankTransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private static final int MAX_RETRIES = 3;

    @Autowired
    public BankTransactionService(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // ================== BASIC FINDS ==================
    @Transactional(readOnly = true)
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountAccountIdOrderByTimestampDesc(accountId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> findByCustomer(Customer customer) {
        return transactionRepository.findByAccountCustomerOrderByTimestampDesc(customer);
    }

    // ================== DEPOSIT ==================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction deposit(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        int attempts = 0;

        while (true) {
            try {
                Account account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
                account.setBalance(currentBalance.add(amount));

                accountRepository.saveAndFlush(account); // ✅ persist account change immediately

                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setTransactionType("DEPOSIT");
                tx.setAmount(amount);
                tx.setTimestamp(LocalDateTime.now());
                tx.setStatus("SUCCESS");

                transactionRepository.saveAndFlush(tx); // ✅ persist transaction immediately
                System.out.println("✅ Deposit successful: Account " + accountId + " new balance = " + account.getBalance());
                return tx;

            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
                if (++attempts >= MAX_RETRIES) throw new RuntimeException("Concurrent deposit failed after retries", e);
                sleepBeforeRetry(attempts);
            }
        }
    }

    // ================== WITHDRAW ==================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        int attempts = 0;

        while (true) {
            try {
                Account account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
                if (currentBalance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for withdrawal");
                }

                account.setBalance(currentBalance.subtract(amount));
                accountRepository.saveAndFlush(account);

                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setTransactionType("WITHDRAW");
                tx.setAmount(amount);
                tx.setTimestamp(LocalDateTime.now());
                tx.setStatus("SUCCESS");

                transactionRepository.saveAndFlush(tx);
                System.out.println("✅ Withdrawal successful: Account " + accountId + " new balance = " + account.getBalance());
                return tx;

            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
                if (++attempts >= MAX_RETRIES) throw new RuntimeException("Concurrent withdrawal failed after retries", e);
                sleepBeforeRetry(attempts);
            }
        }
    }

    // ================== TRANSFER ==================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        validateAmount(amount);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        int attempts = 0;

        while (true) {
            try {
                Account fromAccount = accountRepository.findById(fromAccountId)
                        .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccountId));
                Account toAccount = accountRepository.findById(toAccountId)
                        .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + toAccountId));

                BigDecimal fromBal = fromAccount.getBalance() == null ? BigDecimal.ZERO : fromAccount.getBalance();
                if (fromBal.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for transfer");
                }

                fromAccount.setBalance(fromBal.subtract(amount));
                toAccount.setBalance(toAccount.getBalance() == null ? amount : toAccount.getBalance().add(amount));

                accountRepository.saveAndFlush(fromAccount);
                accountRepository.saveAndFlush(toAccount);

                Transaction debit = new Transaction();
                debit.setAccount(fromAccount);
                debit.setTransactionType("TRANSFER_SENT");
                debit.setAmount(amount);
                debit.setTimestamp(LocalDateTime.now());
                debit.setStatus("SUCCESS");

                Transaction credit = new Transaction();
                credit.setAccount(toAccount);
                credit.setTransactionType("TRANSFER_RECEIVED");
                credit.setAmount(amount);
                credit.setTimestamp(LocalDateTime.now());
                credit.setStatus("SUCCESS");

                transactionRepository.saveAndFlush(debit);
                transactionRepository.saveAndFlush(credit);

                System.out.println("✅ Transfer successful: " +
                        fromAccountId + " → " + toAccountId + " | Amount: " + amount);
                return;

            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
                if (++attempts >= MAX_RETRIES) throw new RuntimeException("Concurrent transfer failed after retries", e);
                sleepBeforeRetry(attempts);
            }
        }
    }

    // ================== LOG TRANSACTION ==================
    @Transactional
    public Transaction logTransaction(Transaction transaction) {
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }
        return transactionRepository.saveAndFlush(transaction);
    }

    // ================== HELPERS ==================
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(100L * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
