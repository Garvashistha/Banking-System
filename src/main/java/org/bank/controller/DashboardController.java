package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.entities.Account;
import org.bank.entities.Transaction;
import org.bank.service.AccountService;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.bank.service.BankTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final AuthService authService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final BankTransactionService transactionService;

    @Autowired
    public DashboardController(AuthService authService,
                               CustomerService customerService,
                               AccountService accountService,
                               BankTransactionService transactionService) {
        this.authService = authService;
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
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
        if (customer == null) {
            model.addAttribute("user", user);
            model.addAttribute("accounts", List.of());
            model.addAttribute("transactions", List.of());
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("totalBalance", BigDecimal.ZERO);
            return "dashboard";
        }

        // 🔹 Always reload accounts directly from DB to get latest balances
        List<Account> accounts = accountService.findByCustomer(customer)
                .stream()
                .map(a -> accountService.findById(a.getAccountId()))
                .toList();

        // 🔹 Load recent transactions freshly and limit to 5 most recent
        List<Transaction> transactions = transactionService.findByCustomer(customer)
                .stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed()) // latest first
                .limit(5) // only last 5
                .collect(Collectors.toList());

        System.out.println("=== Dashboard account balances ===");
        for (Account acc : accounts) {
            System.out.println("Account " + acc.getAccountId() + " → Balance: " + acc.getBalance());
        }

        // 🔹 Quick stats
        int totalAccounts = accounts.size();
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🔹 Add to model
        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("accounts", accounts);
        model.addAttribute("transactions", transactions); // limited to 5
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("requestURI", request.getRequestURI());

        return "dashboard";
    }
}
