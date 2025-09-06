package org.bank.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "MyNewAdminPass123";  // your new password
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println(encodedPassword);
    }
}
