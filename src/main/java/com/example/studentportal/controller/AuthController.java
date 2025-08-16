package com.example.studentportal.controller;

import com.example.studentportal.model.User;
import com.example.studentportal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for authentication-related actions.
 * Handles user registration and login page display.
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AuthController(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Shows the login page.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Shows the registration page with a new user form.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new RegistrationForm());
        return "register";
    }

    /**
     * Processes user registration.
     * Validates form data, creates user, and auto-logs them in.
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") RegistrationForm form,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "register";
        }

        try {
            // Register the user
            User user = userService.registerUser(form.getFullName(), form.getEmail(), form.getPassword());

            // Auto-login the user after successful registration
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            redirectAttributes.addFlashAttribute("message", "Registration successful! Please complete your profile.");
            
            // Redirect to profile completion
            return "redirect:/profile";

        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "register";
        }
    }

    /**
     * Form bean for user registration.
     */
    public static class RegistrationForm {
        private String fullName;
        private String email;
        private String password;

        // Getters and setters
        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}