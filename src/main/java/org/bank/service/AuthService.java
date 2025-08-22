package org.bank.service;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, CustomerService customerService) {
        this.userRepository = userRepository;
        this.customerService = customerService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Load user for Spring Security authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    // Encode password and save user, also create linked Customer (if not present)
    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole("ROLE_USER");  // default role
        }

        User savedUser = userRepository.save(user);

        // Automatically create Customer for normal users if missing
        if ("ROLE_USER".equals(savedUser.getRole())) {
            // Safety: don't create duplicate if something already exists
            Customer existing = customerService.findByUserId(savedUser.getId());
            if (existing == null) {
                Customer customer = new Customer();
                customer.setUser(savedUser);
                customer.setName(savedUser.getUsername()); // single 'name' field in your entity

                // Your Customer has a NOT NULL + UNIQUE email.
                // Use a guaranteed-unique placeholder derived from user id.
                String autoEmail = "u" + savedUser.getId() + "@auto.local";
                customer.setEmail(autoEmail);

                // Optional fields (phone/address) can remain null
                customerService.save(customer);
            }
        }

        return savedUser;
    }

    // Find user by username (optional helper)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
