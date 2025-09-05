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
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CustomerService customerService;
    private final UserService userService;

    @Autowired
    public AccountController(AccountService accountService,
                             CustomerService customerService,
                             UserService userService) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.userService = userService;
    }

    // ================== OPEN ACCOUNT ==================
    @GetMapping("/open")
    public String showOpenAccountForm(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);

        // prepare empty account for form binding (NOT saved yet)
        Account formAccount = new Account();
        formAccount.setBalance(BigDecimal.ZERO);

        model.addAttribute("account", formAccount);
        model.addAttribute("activePage", "open_account");
        return "open_account"; // templates/open_account.html
    }

    @PostMapping("/open")
    public String openAccount(@ModelAttribute("account") Account account,
                              Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);

        // Always create a new account to avoid binding issues with ID
        Account toSave = new Account();
        toSave.setCustomer(customer);

        if (account.getAccountType() == null || account.getAccountType().isBlank()) {
            throw new IllegalArgumentException("Account type is required");
        }
        toSave.setAccountType(account.getAccountType());

        if (account.getBalance() == null) {
            toSave.setBalance(BigDecimal.ZERO);
        } else {
            toSave.setBalance(account.getBalance());
        }

        accountService.save(toSave);
        return "open_account";
    }

    // ================== SAVINGS ACCOUNT ==================
    @GetMapping("/savings")
    public String viewSavingsAccounts(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);
        List<Account> allAccounts = accountService.findByCustomer(customer);

        List<Account> savingsAccounts = allAccounts.stream()
                .filter(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType()))
                .toList();

        model.addAttribute("accounts", savingsAccounts);
        model.addAttribute("activePage", "savings");
        model.addAttribute("customer", customer);
        model.addAttribute("user", customer.getUser());

        return "savings_account";
    }

    // ================== CURRENT ACCOUNT ==================
    @GetMapping("/current")
    public String viewCurrentAccounts(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);
        List<Account> allAccounts = accountService.findByCustomer(customer);

        List<Account> currentAccounts = allAccounts.stream()
                .filter(a -> "CURRENT".equalsIgnoreCase(a.getAccountType()))
                .toList();

        model.addAttribute("accounts", currentAccounts);
        model.addAttribute("activePage", "current");
        model.addAttribute("customer", customer);
        model.addAttribute("user", customer.getUser());

        return "current_account";
    }

    // ================== VIEW BALANCE ==================
    @GetMapping("/balance")
    public String viewBalance(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);
        List<Account> accounts = accountService.findByCustomer(customer);
        model.addAttribute("accounts", accounts);
        model.addAttribute("activePage", "balance");
        model.addAttribute("customer", customer);
        model.addAttribute("user", customer.getUser());

        return "view_balance"; // templates/view_balance.html
    }

    // ================== Helper ==================
    private Customer getCustomerFromAuth(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for user: " + username));
    }
}
