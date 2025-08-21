package org.bank.service;

import org.bank.entities.Customer;
import org.bank.entities.User;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    List<Customer> findAll();

    Optional<Customer> findById(Long id);

    Customer save(Customer customer);

    User save(User user);

    void deleteById(Long id);

    // Find customer by User entity
    Optional<Customer> findByUser(User user);

    // 🔹 Add this method to fix "Cannot resolve method 'findByUserId'"
    Customer findByUserId(Long userId);
}
