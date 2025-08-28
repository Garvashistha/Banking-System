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
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;
    private final CustomerService customerService;
    private final AccountService accountService;

    @Autowired
    public TransactionController(TransactionService transactionService,
                                 AuthService authService,
                                 CustomerService customerService,
                                 AccountService accountService) {
        this.transactionService = transactionService;
        this.authService = authService;
        this.customerService = customerService;
        this.accountService = accountService;
    }

    // ================== TRANSACTION HISTORY ==================
    @GetMapping
    public String listTransactions(Model model, Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Customer customer = customerService.findByUser(user).orElse(null);
        List<Transaction> transactions = (customer != null)
                ? transactionService.findByCustomer(customer)
                : List.of();

        model.addAttribute("transactions", transactions);
        return "transactionhistory";
    }

    // ================== DEPOSIT ==================
    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "deposit_form";
    }

    @PostMapping("/deposit")
    public String processDeposit(@RequestParam Long accountId,
                                 @RequestParam BigDecimal amount,
                                 Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Account account = accountService.findById(accountId).orElse(null);
        if (account == null) return "redirect:/transactions";

        // update balance
        account.setBalance(account.getBalance().add(amount));
        accountService.save(account);

        // save transaction
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("deposit");
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transactionService.save(transaction);

        return "redirect:/transactions";
    }

    // ================== WITHDRAW ==================
    @GetMapping("/withdraw")
    public String showWithdrawForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "withdraw_form";  // ✅ filename fix
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@RequestParam Long accountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Account account = accountService.findById(accountId).orElse(null);
        if (account == null) return "redirect:/transactions";

        if (account.getBalance().compareTo(amount) < 0) {
            // insufficient funds
            return "redirect:/transactions?error=insufficient";
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountService.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("withdraw");
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transactionService.save(transaction);

        return "redirect:/transactions";
    }

    // ================== TRANSFER ==================
    @GetMapping("/transfer")
    public String showTransferForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "transfer_form";
    }

    @PostMapping("/transfer")
    public String processTransfer(@RequestParam Long fromAccountId,
                                  @RequestParam Long toAccountId,
                                  @RequestParam BigDecimal amount,
                                  Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) return "redirect:/login";

        Account fromAccount = accountService.findById(fromAccountId).orElse(null);
        Account toAccount = accountService.findById(toAccountId).orElse(null);

        if (fromAccount == null || toAccount == null) {
            return "redirect:/transactions";
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            return "redirect:/transactions?error=insufficient";
        }

        // update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountService.save(fromAccount);
        accountService.save(toAccount);

        // save transaction
        Transaction transaction = new Transaction();
        transaction.setSourceAccount(fromAccount);
        transaction.setDestinationAccount(toAccount);
        transaction.setTransactionType("transfer");
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transactionService.save(transaction);

        return "redirect:/transactions";
    }

    // ================== HELPER ==================
    private User getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authService.findByUsername(authentication.getName()).orElse(null);
    }
}
