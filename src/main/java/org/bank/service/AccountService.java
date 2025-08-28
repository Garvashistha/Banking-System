package org.bank.service;

import org.bank.entities.Account;
import org.bank.entities.Customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<Account> findAll();
    Optional<Account> findById(Long id);
    Account save(Account account);
    void deleteById(Long id);
    List<Account> findByCustomerId(Long customerId);
    List<Account> findByCustomer(Customer customer);

    // Banking operations
    void deposit(Long accountId, BigDecimal amount);
    void withdraw(Long accountId, BigDecimal amount);
    void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount);
}
