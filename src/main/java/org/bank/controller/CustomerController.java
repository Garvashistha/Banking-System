package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.service.CustomerService;
import org.bank.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;

    @Autowired
    public CustomerController(CustomerService customerService, UserService userService) {
        this.customerService = customerService;
        this.userService = userService;
    }

    // List all customers (for admin usage)
    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        return "customers"; // Only works if you have customers.html
    }

    // Show create form (admin usage)
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer_form"; // Only works if you have customer_form.html
    }

    // Handle customer creation
    @PostMapping("/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer) {
        customerService.save(customer);
        return "redirect:/customers";
    }

    // Show edit form (admin usage)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer Id:" + id));
        model.addAttribute("customer", customer);
        return "customer_form";
    }

    // Handle customer update
    @PostMapping("/update/{id}")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute("customer") Customer customer) {
        customer.setCustomerId(id);
        customerService.save(customer);
        return "redirect:/customers";
    }

    // Delete customer (admin usage)
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteById(id);
        return "redirect:/customers";
    }

    // ================= PROFILE =================

    // View profile (read-only)
    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No customer linked with user: " + username));

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "profile");
        return "profile"; // profile.html (read-only view)
    }

    // Show profile edit form
    @GetMapping("/profile/edit")
    public String editProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer customer = customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No customer linked with user: " + username));

        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "profile");
        return "profile_form"; // new page for editing profile
    }

    // Handle profile update
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("customer") Customer customer, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Customer existingCustomer = customerService.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No customer linked with user: " + username));

        // Update only allowed fields
        existingCustomer.setName(customer.getName());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setPhone(customer.getPhone());
        existingCustomer.setAddress(customer.getAddress());

        customerService.save(existingCustomer);

        return "redirect:/customers/profile";
    }
}
