package org.bank.service;

import org.bank.entities.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    User saveUser(User user);
}
