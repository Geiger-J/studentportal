package com.example.studentportal.controller;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.MatchingService;
import com.example.studentportal.service.RequestService;
import com.example.studentportal.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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

        List<Request> pendingRequests = requestService.getPendingRequests();
        List<Request> matchedRequests = requestService.getMatchedRequests();

        List<User> allUsers = userService.getAllUsers();
        long studentCount = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole())).count();
        long adminCount = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();

        model.addAttribute("admin", admin);
        model.addAttribute("pendingCount", pendingRequests.size());
        model.addAttribute("matchedCount", matchedRequests.size());
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("adminCount", adminCount);

        // Recent requests for dashboard table (non-archived, up to 10)
        List<Request> allRequests = requestService.getAllNonArchivedRequests();
        model.addAttribute("allRequests", allRequests);

        return "admin/dashboard";
    }

    @GetMapping("/requests")
    public String viewRequests(@RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "showArchived", required = false, defaultValue = "false") boolean showArchived,
            Model model) {
        List<Request> requests;
        if (status != null && !status.isEmpty()) {
            if (showArchived) {
                requests = requestService.getRequestsByStatus(status.toUpperCase());
            } else {
                requests = requestService.getNonArchivedRequestsByStatus(status.toUpperCase());
            }
        } else {
            if (showArchived) {
                requests = requestService.getAllRequests();
            } else {
                requests = requestService.getAllNonArchivedRequests();
            }
        }

        model.addAttribute("requests", requests);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("showArchived", showArchived);

        return "admin/requests";
    }

    @PostMapping("/requests/{id}/delete")
    public String deleteRequest(@PathVariable Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "showArchived", required = false, defaultValue = "false") boolean showArchived,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.deleteRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Request deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting request: " + e.getMessage());
        }
        String redirect = "redirect:/admin/requests";
        if (status != null && !status.isEmpty()) {
            redirect += "?status=" + status;
            if (showArchived) redirect += "&showArchived=true";
        } else if (showArchived) {
            redirect += "?showArchived=true";
        }
        return redirect;
    }

    @GetMapping("/users")
    public String viewUsers(@RequestParam(value = "yearGroup", required = false) Integer yearGroup,
            Model model) {
        List<User> users = userService.getUsersByYearGroup(yearGroup);
        model.addAttribute("users", users);
        model.addAttribute("selectedYearGroup", yearGroup);
        return "admin/users";
    }

    @PostMapping("/users/{id}/change-password")
    public String changeUserPassword(@PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
                return "redirect:/admin/users";
            }
            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password changed successfully for user.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error changing password: " + e.getMessage());
        }
        return "redirect:/admin/users";
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

    @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            Model model) {
        model.addAttribute("admin", principal.getUser());
        return "admin/profile";
    }

    @PostMapping("/profile/change-password")
    public String changeAdminPassword(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
                return "redirect:/admin/profile";
            }
            userService.changePassword(principal.getUser().getId(), newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error changing password: " + e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/profile/delete-account")
    public String deleteAdminAccount(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = principal.getUser().getId();
            userService.deleteUser(userId);
            new SecurityContextLogoutHandler().logout(request, response,
                    SecurityContextHolder.getContext().getAuthentication());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting account: " + e.getMessage());
            return "redirect:/admin/profile";
        }
        return "redirect:/login?accountDeleted";
    }
}
