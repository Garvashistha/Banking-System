package org.bank.controller;

import org.bank.entities.Transaction;
import org.bank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public String listTransactions(Model model) {
        List<Transaction> transactions = transactionService.findAll();
        model.addAttribute("transactions", transactions);
        return "transactions"; // Thymeleaf template for transactions history
    }

    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "deposit_form";
    }

    @PostMapping("/deposit")
    public String processDeposit(@ModelAttribute("transaction") Transaction transaction) {
        transaction.setTransactionType("deposit");
        transactionService.save(transaction);
        return "redirect:/transactions";
    }

    @GetMapping("/withdraw")
    public String showWithdrawForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "withdraw_form";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@ModelAttribute("transaction") Transaction transaction) {
        transaction.setTransactionType("withdraw");
        transactionService.save(transaction);
        return "redirect:/transactions";
    }

    @GetMapping("/transfer")
    public String showTransferForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        return "transfer_form";
    }

    @PostMapping("/transfer")
    public String processTransfer(@ModelAttribute("transaction") Transaction transaction) {
        transaction.setTransactionType("transfer");
        transactionService.save(transaction);
        return "redirect:/transactions";
    }
}
