package com.example.studentportal.controller;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.RequestStatus;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller for admin dashboard functionality.
 * Provides admin-only access to system-wide request management.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final RequestService requestService;

    @Autowired
    public AdminController(RequestService requestService) {
        this.requestService = requestService;
    }

    /**
     * Shows the admin dashboard with request management.
     * Supports filtering by archived status via query parameter.
     * 
     * @param archived query parameter to include archived requests (1 = show archived)
     * @param principal authenticated admin user
     * @param model Spring model for template rendering
     * @return admin dashboard template name
     */
    @GetMapping
    public String adminDashboard(@RequestParam(value = "archived", defaultValue = "0") int archived,
                                @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                                Model model) {
        
        User user = principal.getUser();
        
        List<Request> requests;
        boolean showingArchived = archived == 1;
        
        if (showingArchived) {
            // Show all requests (including archived)
            requests = requestService.getAllRequests();
        } else {
            // Show only non-archived requests
            requests = requestService.getAllNonArchivedRequests();
        }
        
        model.addAttribute("user", user);
        model.addAttribute("requests", requests);
        model.addAttribute("showingArchived", showingArchived);
        model.addAttribute("totalRequests", requests.size());
        
        return "admin/dashboard";
    }
}