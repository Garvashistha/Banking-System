package org.bank.repository;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Finds customer by the User's ID (works if Customer has a field 'user')
    Customer findByUserId(Long userId);

    // Finds customer by the User entity itself
    Optional<Customer> findByUser(User user);

    Customer findByUser_Id(Long id);
}
