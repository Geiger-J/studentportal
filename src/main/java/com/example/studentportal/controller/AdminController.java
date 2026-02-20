package com.example.studentportal.controller;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.MatchingService;
import com.example.studentportal.service.RequestService;
import com.example.studentportal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for admin dashboard and management functions.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RequestService requestService;
    private final UserService userService;
    private final MatchingService matchingService;

    @Autowired
    public AdminController(RequestService requestService, UserService userService, MatchingService matchingService) {
        this.requestService = requestService;
        this.userService = userService;
        this.matchingService = matchingService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            Model model) {
        User admin = principal.getUser();

        List<Request> allRequests = requestService.getAllNonArchivedRequests();
        List<Request> pendingRequests = requestService.getPendingRequests();
        List<Request> matchedRequests = requestService.getMatchedRequests();

        List<User> allUsers = userService.getAllUsers();
        long studentCount = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole())).count();
        long adminCount = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();

        model.addAttribute("admin", admin);
        model.addAttribute("allRequests", allRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        model.addAttribute("totalRequests", allRequests.size());
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("adminCount", adminCount);

        return "admin/dashboard";
    }

    @GetMapping("/requests")
    public String viewRequests(@RequestParam(value = "status", required = false) String status,
            Model model) {
        List<Request> requests;
        if (status != null && !status.isEmpty()) {
            requests = requestService.getRequestsByStatus(status.toUpperCase());
        } else {
            requests = requestService.getAllNonArchivedRequests();
        }

        model.addAttribute("requests", requests);
        model.addAttribute("selectedStatus", status);

        return "admin/requests";
    }

    @GetMapping("/users")
    public String viewUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User deleted successfully along with all associated data.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/match")
    public String triggerMatching(RedirectAttributes redirectAttributes) {
        try {
            int matchedCount = matchingService.performMatching();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Matching process completed. " + matchedCount + " requests matched.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Matching process failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/matching/run")
    public String runMatchingAlgorithm(RedirectAttributes redirectAttributes) {
        try {
            int matchedCount = matchingService.performMatching();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Intelligent matching completed. " + matchedCount + " requests matched.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Matching algorithm failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/archive")
    public String archiveOldRequests(RedirectAttributes redirectAttributes) {
        try {
            int archivedCount = requestService.archiveOldRequests();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Archiving completed. " + archivedCount + " requests archived.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Archiving failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
