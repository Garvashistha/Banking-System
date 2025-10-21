package org.bank.controller;

import org.bank.entities.Customer;
import org.bank.entities.User;
import org.bank.repository.CustomerRepository;
import org.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    // ================== SHOW UPDATE PROFILE ==================
    @GetMapping("/profile/update")
    public String showUpdateProfilePage(Model model, Principal principal) {
        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();
        Customer customer = customerRepository.findByUser(user).orElse(null);

        // Prevent null pointer in templates
        if (customer == null) {
            customer = new Customer();
            customer.setUser(user);
        }

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "profile"); // highlight nav if any
        return "updateprofile";
    }
    @GetMapping("/profile/view")
    public String viewProfile(Model model, Principal principal) {
        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        if (optionalUser.isEmpty()) return "redirect:/login";

        User user = optionalUser.get();
        Optional<Customer> optCustomer = customerRepository.findByUser(user);

        optCustomer.ifPresent(customer -> model.addAttribute("customer", customer));
        model.addAttribute("user", user);

        return "view_profile";
    }


    // ================== HANDLE PROFILE UPDATE ==================
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("customer") Customer updatedCustomer,
                                @RequestParam(value = "profileImage", required = false) MultipartFile file,
                                Principal principal,
                                Model model) throws IOException {

        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = optionalUser.get();
        Optional<Customer> optionalCustomer = customerRepository.findByUser(user);

        if (optionalCustomer.isPresent()) {
            Customer customer = optionalCustomer.get();

            // Update basic info
            customer.setName(updatedCustomer.getName());
            customer.setAddress(updatedCustomer.getAddress());
            customer.setPhone(updatedCustomer.getPhone());
            customer.setEmail(updatedCustomer.getEmail());

            // Handle photo upload
            if (file != null && !file.isEmpty()) {
                customer.setProfilePhoto(file.getBytes());
            }

            customerRepository.save(customer);

            model.addAttribute("customer", customer);
            model.addAttribute("user", user);
            model.addAttribute("success", true); // For "Profile updated successfully!" message
        } else {
            model.addAttribute("error", "Customer profile not found!");
        }

        model.addAttribute("activePage", "profile");
        return "updateprofile";
    }

    // ================== SERVE PROFILE PHOTO FROM DATABASE ==================
    @GetMapping("/profile/photo/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getProfilePhoto(@PathVariable("id") Long id) {
        Optional<Customer> optCustomer = customerRepository.findById(id);

        if (optCustomer.isPresent() && optCustomer.get().getProfilePhoto() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(optCustomer.get().getProfilePhoto());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
