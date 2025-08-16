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

/**
 * Controller for authentication-related actions. Handles user registration and
 * login page display.
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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new RegistrationForm());
        return "register";
    }

    /**
     * Processes user registration. Validates form data, creates user, and
     * ensures the user is authenticated immediately.
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") RegistrationForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            // Create the user
            User user = userService.registerUser(form.getFullName(), form.getEmail(), form.getPassword());

            // Load UserDetails and build authentication token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken auth
                    = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // Create and persist a fresh SecurityContext (ensures session persistence)
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            redirectAttributes.addFlashAttribute("message", "Registration successful! Please complete your profile.");
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
