package org.bank.service;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.entities.Transaction;
import org.bank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Override
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    public Account save(Account account) {
        return accountRepository.save(account);
    }

    @Override
    public void deleteById(Long id) {
        accountRepository.deleteById(id);
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        return accountRepository.findByCustomer_CustomerId(customerId);
    }

    @Override
    public List<Account> findByCustomer(Customer customer) {
        return accountRepository.findByCustomer(customer);
    }

    @Override
    public void deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // record transaction
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(BigDecimal.valueOf(amount.doubleValue()));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transactionService.save(transaction);
    }

    @Override
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // record transaction
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("WITHDRAW");
        transaction.setAmount(BigDecimal.valueOf(amount.doubleValue()));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transactionService.save(transaction);
    }

    @Override
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance for transfer");
        }

        // update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // record debit transaction (sender)
        Transaction debit = new Transaction();
        debit.setAccount(fromAccount);
        debit.setTransactionType("TRANSFER_SENT");
        debit.setAmount(BigDecimal.valueOf(amount.doubleValue()));
        debit.setTimestamp(LocalDateTime.now());
        debit.setStatus("SUCCESS");
        transactionService.save(debit);

        // record credit transaction (receiver)
        Transaction credit = new Transaction();
        credit.setAccount(toAccount);
        credit.setTransactionType("TRANSFER_RECEIVED");
        credit.setAmount(BigDecimal.valueOf(amount.doubleValue()));
        credit.setTimestamp(LocalDateTime.now());
        credit.setStatus("SUCCESS");
        transactionService.save(credit);
    }
}
