package org.bank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // ================== BASIC FINDS ==================
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Transaction save(Transaction transaction) {
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountAccountIdOrderByTimestampDesc(accountId);
    }

    // ================== DEPOSIT ==================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction deposit(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        int attempts = 0;
        while (true) {
            try {
                Account acct = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                BigDecimal balance = acct.getBalance() != null ? acct.getBalance() : BigDecimal.ZERO;
                acct.setBalance(balance.add(amount));

                accountRepository.saveAndFlush(acct);

                Transaction tx = new Transaction();
                tx.setAccount(acct);
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
                Account acct = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

                BigDecimal balance = acct.getBalance() != null ? acct.getBalance() : BigDecimal.ZERO;
                if (balance.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for withdrawal");
                }

                acct.setBalance(balance.subtract(amount));
                accountRepository.saveAndFlush(acct);

                Transaction tx = new Transaction();
                tx.setAccount(acct);
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
                Account fromAcct = accountRepository.findById(fromAccountId)
                        .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccountId));
                Account toAcct = accountRepository.findById(toAccountId)
                        .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + toAccountId));

                BigDecimal fromBal = fromAcct.getBalance() != null ? fromAcct.getBalance() : BigDecimal.ZERO;
                if (fromBal.compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient balance for transfer");
                }

                fromAcct.setBalance(fromBal.subtract(amount));
                BigDecimal toBal = toAcct.getBalance() != null ? toAcct.getBalance() : BigDecimal.ZERO;
                toAcct.setBalance(toBal.add(amount));

                accountRepository.saveAndFlush(fromAcct);
                accountRepository.saveAndFlush(toAcct);

                Transaction debit = new Transaction();
                debit.setAccount(fromAcct);
                debit.setAmount(amount);
                debit.setTransactionType("transfer-out");
                debit.setTimestamp(LocalDateTime.now());
                transactionRepository.save(debit);

                Transaction credit = new Transaction();
                credit.setAccount(toAcct);
                credit.setAmount(amount);
                credit.setTransactionType("transfer-in");
                credit.setTimestamp(LocalDateTime.now());
                transactionRepository.save(credit);

                return;
            } catch (OptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
                if (++attempts >= MAX_RETRIES) throw ex;
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
        return transactionRepository.save(transaction);
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
    public List<Transaction> findByCustomer(Customer customer) {
        return transactionRepository.findByAccountCustomerOrderByTimestampDesc(customer);
    }


}
