package org.bank.repository;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomer_CustomerId(Long customerId);
    List<Account> findByCustomer(Customer customer);


}
