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

    // Show update profile page
    @GetMapping("/profile/update")
    public String showUpdateProfilePage(Model model, Principal principal) {
        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        User user = optionalUser.orElse(null);
        Customer customer = null;
        if (user != null) {
            Optional<Customer> optionalCustomer = customerRepository.findByUser(user);
            customer = optionalCustomer.orElse(null);
        }



        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        return "updateprofile";
    }

    // Handle automatic profile update (with optional image)
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute Customer updatedCustomer,
                                @RequestParam(value = "profileImage", required = false) MultipartFile file,
                                Principal principal,
                                Model model) throws IOException {

        Optional<User> optionalUser = userRepository.findByUsername(principal.getName());
        User user = optionalUser.orElse(null);
        Customer customer = null;
        if (user != null) {
            Optional<Customer> optionalCustomer = customerRepository.findByUser(user);
            customer = optionalCustomer.orElse(null);
        }


        if (customer != null) {
            // update basic details
            customer.setName(updatedCustomer.getName());
            customer.setAddress(updatedCustomer.getAddress());
            customer.setPhone(updatedCustomer.getPhone());
            customer.setEmail(updatedCustomer.getEmail());

            // if new photo uploaded
            if (file != null && !file.isEmpty()) {
                customer.setProfilePhoto(file.getBytes());
            }

            customerRepository.save(customer);
        }

        // add to model again so Thymeleaf can render properly
        model.addAttribute("user", user);
        model.addAttribute("customer", customer);
        model.addAttribute("success", true);

        return "updateprofile";
    }

    // Serve profile photo directly from DB
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
