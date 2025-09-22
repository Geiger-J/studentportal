package com.example.studentportal.controller;

import com.example.studentportal.model.*;
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
 * Provides admin-only access to request management, user management, and matching triggers.
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

    /**
     * Admin dashboard showing system overview
     */
    @GetMapping("/dashboard")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                                Model model) {
        User admin = principal.getUser();

        // Get all non-archived requests
        List<Request> allRequests = requestService.getAllNonArchivedRequests();
        List<Request> pendingRequests = requestService.getPendingRequests();
        List<Request> matchedRequests = requestService.getMatchedRequests();
        
        // Get user statistics
        List<User> allUsers = userService.getAllUsers();
        long studentCount = allUsers.stream().filter(u -> u.getRole() == Role.STUDENT).count();
        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();

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

    /**
     * View all requests with filtering options
     */
    @GetMapping("/requests")
    public String viewRequests(@RequestParam(value = "status", required = false) String status,
                              Model model) {
        List<Request> requests;
        if (status != null && !status.isEmpty()) {
            try {
                RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
                requests = requestService.getRequestsByStatus(requestStatus);
            } catch (IllegalArgumentException e) {
                requests = requestService.getAllNonArchivedRequests();
            }
        } else {
            requests = requestService.getAllNonArchivedRequests();
        }

        model.addAttribute("requests", requests);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("allStatuses", RequestStatus.values());

        return "admin/requests";
    }

    /**
     * View all users
     */
    @GetMapping("/users")
    public String viewUsers(Model model) {
        List<User> users = userService.getAllUsers();
        
        model.addAttribute("users", users);

        return "admin/users";
    }

    /**
     * Delete a user and all their associated data
     */
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

    /**
     * Manual matching trigger - uses the matching service
     */
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

    /**
     * Run intelligent matching algorithm
     */
    @PostMapping("/matches/run")
    public String runMatchingAlgorithm(RedirectAttributes redirectAttributes) {
        try {
            List<MatchingService.Match> matches = matchingService.runMatching();
            
            // Process the matches
            int matchedCount = 0;
            for (MatchingService.Match match : matches) {
                Request offerRequest = match.getOfferRequest();
                Request seekRequest = match.getSeekRequest();
                
                // Update status and matched partners  
                offerRequest.setStatus(RequestStatus.MATCHED);
                offerRequest.setMatchedPartner(seekRequest.getUser());
                
                seekRequest.setStatus(RequestStatus.MATCHED);
                seekRequest.setMatchedPartner(offerRequest.getUser());
                
                matchedCount += 2;
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Intelligent matching completed. " + matchedCount + " requests matched with " + matches.size() + " optimal pairs.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Matching algorithm failed: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    /**
     * Archive old requests manually using the matching service
     */
    @PostMapping("/archive")
    public String archiveOldRequests(RedirectAttributes redirectAttributes) {
        try {
            int archivedCount = matchingService.performArchival();
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Archived " + archivedCount + " old requests.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Archive process failed: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }
}