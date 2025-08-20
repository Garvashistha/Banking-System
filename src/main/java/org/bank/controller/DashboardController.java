package org.bank.controller;


import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.entities.Account;
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
import java.util.Collections;
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

        model.addAttribute("user", user);
        model.addAttribute("activePage", "dashboard"); // sidebar highlight
        model.addAttribute("requestURI", request.getRequestURI()); // avoid #request null

        if ("ROLE_ADMIN".equals(user.getRole())) {
            List<Customer> customers = customerService.findAll();
            model.addAttribute("customers", customers != null ? customers : Collections.emptyList());
        } else {
            Customer customer = customerService.findByUserId(user.getId());
            model.addAttribute("customer", customer);

            if (customer != null) {
                List<Account> accounts = accountService.findByCustomerId(customer.getCustomerId());
                model.addAttribute("accounts", accounts != null ? accounts : Collections.emptyList());

                if (accounts != null && !accounts.isEmpty()) {
                    model.addAttribute("transactions",
                            transactionService.findByAccountId(accounts.get(0).getAccountId()));
                } else {
                    model.addAttribute("transactions", Collections.emptyList());
                }
            } else {
                model.addAttribute("accounts", Collections.emptyList());
                model.addAttribute("transactions", Collections.emptyList());
            }
        }

        return "dashboard";
    }
}
