package org.bank.service;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -----------------------------
    // Spring Security login method
    // -----------------------------
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole())))
                .build();
    }

    // -----------------------------
    // Save user and auto-create Customer
    // -----------------------------
    public User saveUser(User user, CustomerService customerService) {
        // Encode password if not already encoded
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Set default role if not set
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        // Save user
        User savedUser = userRepository.save(user);

        // Auto-create Customer for normal users if not already linked
        if ("ROLE_USER".equals(savedUser.getRole())) {
            Customer existingCustomer = customerService.findByUserId(savedUser.getId());
            if (existingCustomer == null) {
                Customer customer = new Customer();
                customer.setUser(savedUser);
                customer.setName(savedUser.getUsername());
                customer.setEmail("u" + savedUser.getId() + "@auto.local");
                customerService.save(customer);
            }
        }

        return savedUser;
    }

    // -----------------------------
    // Helper to fetch user by username
    // -----------------------------
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
