package com.example.studentportal.controller;

import com.example.studentportal.model.*;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.RequestService;
import com.example.studentportal.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for tutoring request management.
 * Handles request creation and cancellation.
 */
@Controller
@RequestMapping("/requests")
public class RequestController {

    private final RequestService requestService;
    private final SubjectService subjectService;

    @Autowired
    public RequestController(RequestService requestService, SubjectService subjectService) {
        this.requestService = requestService;
        this.subjectService = subjectService;
    }

    /**
     * Shows the request creation form.
     * Redirects to profile if profile is not complete.
     * Forbids access for ADMIN users.
     */
    @GetMapping("/new")
    public String showRequestForm(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        
        User user = principal.getUser();

        // Block admin users from accessing request creation
        if (user.getRole() == Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", 
                "Administrators cannot create tutoring requests. Please use the admin dashboard to manage the system.");
            return "redirect:/admin/dashboard";
        }

        // Check if profile is complete
        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
        model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));

        return "request_form";
    }

    /**
     * Processes request creation.
     * Validates data and creates the request.
     * Forbids access for ADMIN users.
     */
    @PostMapping
    public String createRequest(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @RequestParam RequestType type,
                               @RequestParam Long subjectId,
                               @RequestParam List<Timeslot> timeslots,
                               @RequestParam(defaultValue = "false") Boolean recurring,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        User user = principal.getUser();

        // Block admin users from creating requests
        if (user.getRole() == Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", 
                "Administrators cannot create tutoring requests.");
            return "redirect:/admin/dashboard";
        }

        // Check if profile is complete
        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        try {
            // Validate subject
            Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject selected"));

            // Validate timeslots
            if (timeslots == null || timeslots.isEmpty()) {
                model.addAttribute("error", "Please select at least one timeslot");
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
                model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));
                return "request_form";
            }

            Set<Timeslot> timeslotSet = new HashSet<>(timeslots);

            // Create the request
            Request request = requestService.createRequest(user, type, subject, timeslotSet, recurring);

            redirectAttributes.addFlashAttribute("message", 
                "Request created successfully! Your " + type.getDisplayName().toLowerCase() + 
                " request for " + subject.getDisplayName() + " has been submitted.");

            return "redirect:/dashboard";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
            model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));
            return "request_form";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating request: " + e.getMessage());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
            model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));
            return "request_form";
        }
    }

    /**
     * Cancels a request.
     * Only allows cancelling user's own PENDING requests.
     */
    @PostMapping("/{id}/cancel")
    public String cancelRequest(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        
        User user = principal.getUser();

        try {
            Request cancelledRequest = requestService.cancelRequest(id, user);
            
            redirectAttributes.addFlashAttribute("message", 
                "Request cancelled successfully. Your " + 
                cancelledRequest.getType().getDisplayName().toLowerCase() + 
                " request for " + cancelledRequest.getSubject().getDisplayName() + 
                " has been cancelled.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling request: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}