package org.bank.service;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.repository.CustomerRepository;
import org.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;  // Needed to save User

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);  // Implementation for save(User)
    }

    @Override
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    @Override
    public Optional<Customer> findByUser(User user) {
        return customerRepository.findByUser(user); // Requires method in CustomerRepository
    }

    @Override
    public Customer findByUserId(Long userId) {
        return customerRepository.findByUserId(userId); // Requires method in CustomerRepository
    }
}
