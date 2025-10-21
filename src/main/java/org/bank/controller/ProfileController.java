package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.repository.CustomerRepository;
import org.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Controller
public class ProfileController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProfileController(CustomerRepository customerRepository,
                             UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    /**
     * View profile (read-only)
     * URL: GET /profile/view
     */
    @GetMapping("/profile/view")
    public String viewProfile(Model model, Principal principal) {
        // principal may be null in some testing contexts; handle defensively
        User user = null;
        Customer customer = null;

        if (principal != null && principal.getName() != null) {
            Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                Optional<Customer> optionalCustomer = customerRepository.findByUser(user);
                customer = optionalCustomer.orElse(null);
            }
        }

        // Add to model even if null – templates should render safely
        model.addAttribute("user", user);
        model.addAttribute("customer", customer);

        return "view_profile";
    }

    /**
     * Show update profile page (GET /profile/update)
     * Uses the same template name you already have: updateprofile.html
     */
    @GetMapping("/profile/update")
    public String showUpdateProfilePage(Model model, Principal principal) {
        User user = null;
        Customer customer = null;

        if (principal != null && principal.getName() != null) {
            Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                Optional<Customer> optionalCustomer = customerRepository.findByUser(user);
                customer = optionalCustomer.orElse(null);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);

        return "updateprofile";
    }

    /**
     * Update profile (POST /profile/update)
     * Accepts optional file upload "profileImage". Saves data automatically to DB.
     */
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute Customer updatedCustomer,
                                @RequestParam(value = "profileImage", required = false) MultipartFile file,
                                Principal principal,
                                Model model) throws IOException {

        if (principal == null || principal.getName() == null) {
            // Not authenticated — redirect to login (adjust if your auth flow differs)
            return "redirect:/login";
        }

        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();
        Optional<Customer> optionalCustomer = customerRepository.findByUser(user);
        Customer customer = optionalCustomer.orElse(null);

        if (customer != null) {
            // update fields only when provided (basic overwrite)
            customer.setName(updatedCustomer.getName());
            customer.setAddress(updatedCustomer.getAddress());
            customer.setPhone(updatedCustomer.getPhone());
            customer.setEmail(updatedCustomer.getEmail());

            if (file != null && !file.isEmpty()) {
                // save bytes in DB field customer.profilePhoto (byte[])
                customer.setProfilePhoto(file.getBytes());
            }

            customerRepository.save(customer);
        }

        // put updated values back into model for the template
        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("success", true);

        // return update view (you can redirect if you'd prefer flash attributes + redirect)
        return "updateprofile";
    }

    /**
     * Serve profile photo bytes from the DB
     * GET /profile/photo/{id}
     */
    @GetMapping("/profile/photo/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> getProfilePhoto(@PathVariable("id") Long id) {
        Optional<Customer> optCustomer = customerRepository.findById(id);

        if (optCustomer.isPresent() && optCustomer.get().getProfilePhoto() != null) {
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                    .body(optCustomer.get().getProfilePhoto());
        } else {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }
}
