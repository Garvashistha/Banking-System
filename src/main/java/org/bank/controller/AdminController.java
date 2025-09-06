package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.entities.Account;
import org.bank.entities.Transaction;
import org.bank.service.CustomerService;
import org.bank.service.AuthService;
import org.bank.service.AccountService;
import org.bank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CustomerService customerService;
    private final AuthService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @Autowired
    public AdminController(CustomerService customerService,
                           AuthService authService,
                           AccountService accountService,
                           TransactionService transactionService) {
        this.customerService = customerService;
        this.authService = authService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    // ================== ADMIN DASHBOARD ==================
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = authService.findByUsername(username).orElse(null);

        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/dashboard"; // fallback for non-admins
        }

        // Customers
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers != null ? customers : Collections.emptyList());

        // Accounts
        List<Account> accounts = accountService.findAll();
        model.addAttribute("accounts", accounts != null ? accounts : Collections.emptyList());

        // Transactions
        List<Transaction> transactions = transactionService.findAll();
        model.addAttribute("transactions", transactions != null ? transactions : Collections.emptyList());

        // Admin user info
        model.addAttribute("user", user);
        model.addAttribute("activePage", "admin-dashboard");

        // Return template directly from templates/ folder
        return "admin_dashboard";
    }

    // ================== CUSTOMER CRUD ==================
    @GetMapping("/customers")
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        model.addAttribute("activePage", "customers");
        return "customers"; // remove admin/ prefix
    }

    @GetMapping("/customers/create")
    public String showCreateCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer_form"; // remove admin/ prefix
    }

    @PostMapping("/customers/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer) {
        customerService.save(customer);
        return "redirect:/admin/customers";
    }

    @GetMapping("/customers/edit/{id}")
    public String showEditCustomerForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer Id:" + id));
        model.addAttribute("customer", customer);
        return "customer_form"; // remove admin/ prefix
    }

    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute("customer") Customer customer) {
        customer.setCustomerId(id);
        customerService.save(customer);
        return "redirect:/admin/customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteById(id);
        return "redirect:/admin/customers";
    }

    // ================== ACCOUNT CRUD ==================
    @GetMapping("/accounts")
    public String listAccounts(Model model) {
        List<Account> accounts = accountService.findAll();
        model.addAttribute("accounts", accounts);
        model.addAttribute("activePage", "accounts");
        return "accounts"; // remove admin/ prefix
    }

    @GetMapping("/accounts/edit/{id}")
    public String showEditAccountForm(@PathVariable Long id, Model model) {
        Account account = accountService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid account Id:" + id));
        model.addAttribute("account", account);
        return "account_form"; // remove admin/ prefix
    }

    @PostMapping("/accounts/update/{id}")
    public String updateAccount(@PathVariable Long id, @ModelAttribute("account") Account account) {
        account.setAccountId(id);
        accountService.save(account);
        return "redirect:/admin/accounts";
    }

    @GetMapping("/accounts/delete/{id}")
    public String deleteAccount(@PathVariable Long id) {
        accountService.deleteById(id);
        return "redirect:/admin/accounts";
    }

    // ================== TRANSACTIONS (READ-ONLY) ==================
    @GetMapping("/transactions")
    public String listTransactions(Model model) {
        List<Transaction> transactions = transactionService.findAll();
        model.addAttribute("transactions", transactions);
        model.addAttribute("activePage", "transactions");
        return "transactions"; // remove admin/ prefix
    }
}
