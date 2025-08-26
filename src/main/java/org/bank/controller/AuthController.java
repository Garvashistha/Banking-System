package org.bank.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.bank.entities.User;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    @Autowired
    public AuthController(AuthService authService, CustomerService customerService) {
        this.authService = authService;
        this.customerService = customerService;
    }

    // -----------------------------
    // Login page
    // -----------------------------
    @GetMapping({"/login"})
    public String showLoginForm() {
        return "login"; // templates/login.html
    }

    // -----------------------------
    // Registration page
    // -----------------------------
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // templates/register.html
    }

    // -----------------------------
    // Handle registration
    // -----------------------------
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("confirmPassword") String confirmPassword,
                               Model model,
                               RedirectAttributes ra) {

        // Check if username already exists
        if (authService.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        // Check password confirmation
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        // Set default role if none provided
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        // Save user and auto-create customer
        User savedUser = authService.saveUser(user, customerService);

        // Flash registered username to success page
        ra.addFlashAttribute("registeredUsername", savedUser.getUsername());
        return "redirect:/register/success";
    }

    // -----------------------------
    // Registration success page
    // -----------------------------
    @GetMapping("/register/success")
    public String registrationSuccess(Model model) {
        if (!model.containsAttribute("registeredUsername")) {
            model.addAttribute("registeredUsername", "your account");
        }
        return "register_success"; // templates/register_success.html
    }

    // -----------------------------
    // Logout
    // -----------------------------
    @GetMapping("/logout")
    public String logout() {
        SecurityContextHolder.clearContext();
        return "redirect:/login?logout";
    }
}
