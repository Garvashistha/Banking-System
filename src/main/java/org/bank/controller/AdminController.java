package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.service.CustomerService;
import org.bank.service.AuthService;
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

    @Autowired
    public AdminController(CustomerService customerService, AuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    // ================== ADMIN DASHBOARD ==================
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = authService.findByUsername(username).orElse(null);

        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/dashboard"; // fallback for non-admins
        }

        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers != null ? customers : Collections.emptyList());
        model.addAttribute("activePage", "admin-dashboard");

        return "admin/admin_dashboard";
        // templates/admin/admin_dashboard.html
    }

    // ================== CUSTOMER CRUD (ADMIN ONLY) ==================
    @GetMapping("/customers")
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        model.addAttribute("activePage", "customers");
        return "admin/customers";
        // templates/admin/customers.html
    }

    @GetMapping("/customers/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "admin/customer_form";
        // templates/admin/customer_form.html
    }

    @PostMapping("/customers/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer) {
        customerService.save(customer);
        return "redirect:/admin/customers";
    }

    @GetMapping("/customers/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer Id:" + id));
        model.addAttribute("customer", customer);
        return "admin/customer_form";
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
}
