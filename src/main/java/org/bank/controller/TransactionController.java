package org.bank.controller;

import org.bank.entities.Account;
import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.service.AccountService;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.bank.service.BankTransactionService;
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

    private final BankTransactionService transactionService;
    private final AuthService authService;
    private final AccountService accountService;
    private final CustomerService customerService;

    @Autowired
    public TransactionController(BankTransactionService transactionService,
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
        List<org.bank.entities.Transaction> transactions = new ArrayList<>();

        for (Account account : accounts) {
            transactions.addAll(transactionService.findByAccountId(account.getAccountId()));
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "transactionhistory");

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
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "deposit");

        return "deposit_form";
    }

    @PostMapping("/deposit")
    public String processDeposit(@RequestParam Long accountId,
                                 @RequestParam BigDecimal amount,
                                 Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        try {
            transactionService.deposit(accountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/transactions/deposit?error=" + e.getMessage();
        }

        return "redirect:/dashboard?success=Deposit+completed";
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
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "withdraw");

        return "withdraw_form";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@RequestParam Long accountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        try {
            transactionService.withdraw(accountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/transactions/withdraw?error=" + e.getMessage();
        }

        return "redirect:/dashboard?success=Withdrawal+completed";
    }

    // ================== TRANSFER ==================
    @GetMapping("/transfer")
    public String showTransferForm(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUserId(user.getId());
        if (customer == null) return "redirect:/login";

        List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());
        List<Account> allAccounts = accountService.findAll();

        model.addAttribute("accounts", accounts);
        model.addAttribute("allAccounts", allAccounts);
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "transfer");

        return "transfer_form";
    }

    @PostMapping("/transfer")
    public String processTransfer(@RequestParam Long fromAccountId,
                                  @RequestParam Long toAccountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        try {
            transactionService.transfer(fromAccountId, toAccountId, amount);
        } catch (RuntimeException e) {
            return "redirect:/transactions/transfer?error=" + e.getMessage();
        }

        return "redirect:/dashboard?success=Transfer+completed";
    }

    // ================== HELPER ==================
    private User getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authService.findByUsername(authentication.getName()).orElse(null);
    }
}
