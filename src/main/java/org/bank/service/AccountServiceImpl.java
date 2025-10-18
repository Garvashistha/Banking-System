package org.bank.service;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final BankTransactionService transactionService;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              BankTransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Override
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Override
    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
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

    // ✅ Delegate balance operations to TransactionService (single source of truth)

    @Override
    public void deposit(Long accountId, BigDecimal amount) {
        transactionService.deposit(accountId, amount);
    }

    @Override
    public void withdraw(Long accountId, BigDecimal amount) {
        transactionService.withdraw(accountId, amount);
    }

    @Override
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        transactionService.transfer(fromAccountId, toAccountId, amount);
    }
}
