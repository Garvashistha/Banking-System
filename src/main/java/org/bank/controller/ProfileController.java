package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.service.AuthService;
import org.bank.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

    private final CustomerService customerService;
    private final AuthService authService;

    @Autowired
    public ProfileController(CustomerService customerService, AuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    // ================== VIEW PROFILE ==================
    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        User user = authService.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        Customer customer = customerService.findByUserId(user.getId());
        model.addAttribute("customer", customer);
        return "profile";
    }

    // ================== UPDATE PROFILE ==================
    @GetMapping("/profile/update")
    public String showUpdateForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        User user = authService.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        Customer customer = customerService.findByUserId(user.getId());
        model.addAttribute("customer", customer);
        return "updateprofile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String address,
                                @RequestParam String phone,
                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        User user = authService.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        Customer customer = customerService.findByUserId(user.getId());
        if (customer != null) {
            customer.setName(name);
            customer.setAddress(address);
            customer.setPhone(phone);
            customerService.save(customer);
        }

        return "redirect:/profile";
    }
}
