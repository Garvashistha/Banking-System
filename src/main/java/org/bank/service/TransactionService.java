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

    // ================== DEPOSIT ==================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction deposit(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        int attempts = 0;

        while (true) {
            try {
                Account account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
                account.setBalance(balance.add(amount));
                accountRepository.save(account);

                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setAmount(amount);
                tx.setTransactionType("deposit");
                tx.setTimestamp(LocalDateTime.now());
                return transactionRepository.save(tx);
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
                if (++attempts >= MAX_RETRIES) throw ex;
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

                BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
                if (balance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for withdrawal");
                }
                account.setBalance(balance.subtract(amount));
                accountRepository.save(account);

                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setAmount(amount);
                tx.setTransactionType("withdraw");
                tx.setTimestamp(LocalDateTime.now());
                return transactionRepository.save(tx);
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
                if (++attempts >= MAX_RETRIES) throw ex;
                sleepBeforeRetry(attempts);
            }
        }
    }

    @Transactional
    public Transaction logTransaction(Transaction transaction) {
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
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

                BigDecimal fromBalance = fromAccount.getBalance() != null ? fromAccount.getBalance() : BigDecimal.ZERO;
                if (fromBalance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for transfer");
                }

                // update balances
                fromAccount.setBalance(fromBalance.subtract(amount));
                BigDecimal toBalance = toAccount.getBalance() != null ? toAccount.getBalance() : BigDecimal.ZERO;
                toAccount.setBalance(toBalance.add(amount));
                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                // create transaction records
                Transaction debitTx = new Transaction();
                debitTx.setAccount(fromAccount);
                debitTx.setAmount(amount);
                debitTx.setTransactionType("transfer-out");
                debitTx.setTimestamp(LocalDateTime.now());
                transactionRepository.save(debitTx);

                Transaction creditTx = new Transaction();
                creditTx.setAccount(toAccount);
                creditTx.setAmount(amount);
                creditTx.setTransactionType("transfer-in");
                creditTx.setTimestamp(LocalDateTime.now());
                transactionRepository.save(creditTx);

                return;
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
                if (++attempts >= MAX_RETRIES) throw ex;
                sleepBeforeRetry(attempts);
            }
        }
    }

    // ================== FIND METHODS ==================
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
