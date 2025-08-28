package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.entities.Account;
import org.bank.entities.Transaction;
import org.bank.service.AccountService;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.bank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {

    private final AuthService authService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @Autowired
    public DashboardController(AuthService authService,
                               CustomerService customerService,
                               AccountService accountService,
                               TransactionService transactionService) {
        this.authService = authService;
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication, HttpServletRequest request) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        User user = authService.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // 🔹 Load customer
        Customer customer = customerService.findByUser(user).orElse(null);

        // 🔹 Load accounts
        List<Account> accounts = (customer != null)
                ? accountService.findByCustomer(customer)
                : List.of();

        // 🔹 Load transactions
        List<Transaction> transactions = (customer != null)
                ? transactionService.findByCustomer(customer)
                : List.of();

        // 🔹 Quick stats
        int totalAccounts = accounts.size();
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🔹 Add to model
        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("accounts", accounts);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("activePage", "dashboard"); // sidebar highlight
        model.addAttribute("requestURI", request.getRequestURI());

        return "dashboard";
    }
}
