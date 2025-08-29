package org.bank.controller;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.entities.Transaction;
import org.bank.entities.User;
import org.bank.service.AccountService;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.bank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;
    private final AccountService accountService;
    private final CustomerService customerService;

    @Autowired
    public TransactionController(TransactionService transactionService,
                                 AuthService authService,
                                 AccountService accountService,
                                 CustomerService customerService) {
        this.transactionService = transactionService;
        this.authService = authService;
        this.accountService = accountService;
        this.customerService = customerService;
    }

    // ================== TRANSACTION HISTORY ==================
    @GetMapping("/transactionhistory")
    public String listTransactions(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUserId(user.getId());
        if (customer == null) return "redirect:/login";

        List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());
        List<Transaction> transactions = new ArrayList<>();

        for (Account account : accounts) {
            transactions.addAll(transactionService.findByAccountId(account.getAccountId()));
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("user", user);
        return "transactionhistory";
    }


    // ================== DEPOSIT ==================
    @GetMapping("/deposit")
    public String showDepositForm(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUserId(user.getId());
        if (customer == null) return "redirect:/login";

        List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());
        model.addAttribute("accounts", accounts);
        model.addAttribute("user", user); // 🔥 Added

        return "deposit_form";
    }

    @PostMapping("/deposit")
    public String processDeposit(@RequestParam Long accountId,
                                 @RequestParam BigDecimal amount,
                                 Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        try {
            accountService.deposit(accountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/transactions?error=" + e.getMessage();
        }

        return "redirect:/dashboard";
    }

    // ================== WITHDRAW ==================
    @GetMapping("/withdraw")
    public String showWithdrawForm(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUserId(user.getId());
        if (customer == null) return "redirect:/login";

        List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());
        model.addAttribute("accounts", accounts);
        model.addAttribute("user", user); // 🔥 Added

        return "withdraw_form";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@RequestParam Long accountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        try {
            accountService.withdraw(accountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }

        return "redirect:/dashboard";
    }

    // ================== TRANSFER ==================
    @GetMapping("/transfer")
    public String showTransferForm(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUserId(user.getId());
        if (customer == null) return "redirect:/login";

        // Source accounts: only for the logged-in customer
        List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());

        // Destination accounts: all accounts in the system
        List<Account> allAccounts = accountService.findAll();

        model.addAttribute("accounts", accounts);
        model.addAttribute("allAccounts", allAccounts);
        model.addAttribute("user", user); // 🔥 Added

        return "transfer_form";
    }

    @PostMapping("/transfer")
    public String processTransfer(@RequestParam Long fromAccountId,
                                  @RequestParam Long toAccountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";
        if (fromAccountId.equals(toAccountId)) {
            return "redirect:/transactions/transfer?error=Cannot+transfer+to+the+same+account";
        }

        try {
            accountService.transfer(fromAccountId, toAccountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }

        return "redirect:/dashboard";
    }

    // ================== HELPER ==================
    private User getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authService.findByUsername(authentication.getName()).orElse(null);
    }
}
