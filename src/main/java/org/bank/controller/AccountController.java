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
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for user: " + username));

        Account formAccount = new Account();
        formAccount.setCustomer(customer);
        formAccount.setBalance(BigDecimal.ZERO);

        model.addAttribute("account", formAccount);
        model.addAttribute("activePage", "open_account");
        return "open_account"; // templates/open_account.html
    }

    @PostMapping("/open")
    public String openAccount(@ModelAttribute("account") Account account,
                              Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for user: " + username));

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

    // ================== SAVINGS ACCOUNT ==================
    @GetMapping("/savings")
    public String viewSavingsAccounts(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);
        List<Account> allAccounts = accountService.findByCustomer(customer);

        // filter in memory instead of repository
        List<Account> savingsAccounts = allAccounts.stream()
                .filter(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType()))
                .toList();

        model.addAttribute("accounts", savingsAccounts);
        model.addAttribute("activePage", "savings");
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
        return "current_account";
    }

    // ================== VIEW BALANCE ==================
    @GetMapping("/balance")
    public String viewBalance(Model model, Authentication authentication) {
        Customer customer = getCustomerFromAuth(authentication);
        List<Account> accounts = accountService.findByCustomer(customer);
        model.addAttribute("accounts", accounts);
        model.addAttribute("activePage", "balance");
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
