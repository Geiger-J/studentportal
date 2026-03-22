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

// Controller: student dashboard view
//
// - display user's active tutoring requests
// - redirect admins to admin dashboard
// - redirect students with incomplete profiles to profile setup
@Controller
public class DashboardController {

    private final RequestService requestService;

    @Autowired
    public DashboardController(RequestService requestService) {
        this.requestService = requestService;
    }

    // role-based redirect -> profile guard -> load requests -> render
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            @RequestParam(value = "showArchived", required = false, defaultValue = "false") boolean showArchived,
            Model model) {

        User user = principal.getUser();

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }

        // incomplete profile -> force profile setup before accessing dashboard
        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        List<Request> userRequests = requestService.getUserRequests(user, showArchived);

        model.addAttribute("user", user);
        model.addAttribute("requests", userRequests);
        model.addAttribute("showArchived", showArchived);

        return "dashboard";
    }
}