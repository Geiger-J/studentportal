package com.example.studentportal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.studentportal.model.User;
import com.example.studentportal.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

// Controller - registration and login page endpoints
//
// Responsibilities:
// - serve login page
// - handle registration form submission
// - auto-authenticate user after registration
@Controller
public class AuthController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AuthController(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new RegistrationForm());
        return "register";
    }

    // validate form → create user → auto-authenticate → redirect to profile
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") RegistrationForm form,
            BindingResult result, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            User user = userService.registerUser(form.getFullName(), form.getEmail(),
                    form.getPassword());

            // load UserDetails → build auth token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // new SecurityContext → persist in session [ensures auth survives redirect]
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);

            redirectAttributes.addFlashAttribute("message",
                    "Registration successful! Please complete your profile.");
            return "redirect:/profile";

        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "register";
        }
    }

    // form bean for registration [fullName, email, password]
    public static class RegistrationForm {

        private String fullName;
        private String email;
        private String password;

        // --- accessors
        public String getFullName() { return fullName; }

        // --- mutators
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }

        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }

        public void setPassword(String password) { this.password = password; }
    }
}
