package com.example.studentportal.controller;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller for the user dashboard.
 * Shows user's requests and provides navigation to other features.
 */
@Controller
public class DashboardController {

    private final RequestService requestService;

    @Autowired
    public DashboardController(RequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Shows the user dashboard with their requests.
     * Redirects ADMIN users to /admin/dashboard.
     * Redirects STUDENT users to profile completion if profile is not complete.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                           @RequestParam(value = "showArchived", required = false, defaultValue = "false") boolean showArchived,
                           Model model) {
        
        User user = principal.getUser();

        // Redirect ADMIN users to admin dashboard
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        // Check if profile is complete for STUDENT users - redirect to profile if not
        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        // Get user's requests
        List<Request> userRequests = requestService.getUserRequests(user, showArchived);

        model.addAttribute("user", user);
        model.addAttribute("requests", userRequests);
        model.addAttribute("showArchived", showArchived);

        return "dashboard";
    }
}