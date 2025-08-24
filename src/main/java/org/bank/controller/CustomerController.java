package org.bank.controller;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.service.AccountService;
import org.bank.service.CustomerService;
import org.bank.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;
    private final AccountService accountService;

    @Autowired
    public CustomerController(CustomerService customerService,
                              UserService userService,
                              AccountService accountService) {
        this.customerService = customerService;
        this.userService = userService;
        this.accountService = accountService;
    }

    // ================== DASHBOARD ==================

    // ================== CUSTOMER CRUD ==================
    @GetMapping("/customers")
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        model.addAttribute("activePage", "customers");
        return "customers";
    }

    @GetMapping("/customers/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer_form";
    }

    @PostMapping("/customers/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer) {
        customerService.save(customer);
        return "redirect:/customers";
    }

    @GetMapping("/customers/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer Id:" + id));
        model.addAttribute("customer", customer);
        return "customer_form";
    }

    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute("customer") Customer customer) {
        customer.setCustomerId(id);
        customerService.save(customer);
        return "redirect:/customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteById(id);
        return "redirect:/customers";
    }

    // ================== PROFILE ==================
    @GetMapping("/customers/profile")
    public String viewProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseGet(() -> autoCreateCustomer(user));

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "profile");
        return "profile";
    }

    @GetMapping("/customers/profile/edit")
    public String editProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseGet(() -> autoCreateCustomer(user));

        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "profile");
        return "profile_form";
    }

    @PostMapping("/customers/profile/update")
    public String updateProfile(@ModelAttribute("customer") Customer customer, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer existingCustomer = customerService.findByUser(user)
                .orElseGet(() -> autoCreateCustomer(user));

        existingCustomer.setName(customer.getName());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setPhone(customer.getPhone());
        existingCustomer.setAddress(customer.getAddress());

        customerService.save(existingCustomer);
        return "redirect:/customers/profile";
    }

    // ================== OPEN ACCOUNT ==================
    @GetMapping("/customers/open-account")
    public String showOpenAccountForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseGet(() -> autoCreateCustomer(user));

        // Prepare unsaved Account for the form (do not persist here!)
        Account formAccount = new Account();
        formAccount.setCustomer(customer);
        formAccount.setBalance(BigDecimal.ZERO);

        model.addAttribute("account", formAccount);
        model.addAttribute("activePage", "open-account");
        return "open_account";
    }

    @PostMapping("/customers/open-account")
    public String openAccount(@ModelAttribute("account") Account account,
                              Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseGet(() -> autoCreateCustomer(user));

        // attach correct customer and validate
        account.setCustomer(customer);
        if (account.getAccountType() == null || account.getAccountType().isBlank()) {
            throw new IllegalArgumentException("Account type is required");
        }
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }

        accountService.save(account);

        return "redirect:/dashboard";
    }

    // ================== Helper ==================
    private Customer autoCreateCustomer(User user) {
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setName(user.getUsername());
        customer.setEmail("u" + user.getId() + "@auto.local"); // unique placeholder email
        return customerService.save(customer);
    }
}
