package com.example.studentportal.controller;

import com.example.studentportal.model.*;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.RequestService;
import com.example.studentportal.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for tutoring request management.
 * Handles request creation and cancellation.
 * Only accessible to STUDENT role users.
 */
@Controller
@RequestMapping("/requests")
@PreAuthorize("hasRole('STUDENT')")
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
     */
    @GetMapping("/new")
    public String showRequestForm(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                                 Model model) {
        
        User user = principal.getUser();

        // Check if profile is complete
        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        // Convert userAvailability Set<Timeslot> to Set<String> for template usage
        Set<String> userAvailabilityNames = user.getAvailability() == null ? 
            new HashSet<>() : 
            user.getAvailability().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // Show only subjects that the user has in their profile (curated subjects)
        model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
        model.addAttribute("userAvailability", user.getAvailability());
        model.addAttribute("userAvailabilityNames", userAvailabilityNames);
        model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
        model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));

        return "request_form";
    }

    /**
     * Processes request creation.
     * Validates data and creates the request.
     */
    @PostMapping
    public String createRequest(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @RequestParam RequestType type,
                               @RequestParam Long subjectId,
                               @RequestParam List<Timeslot> timeslots,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        User user = principal.getUser();

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
                model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
                model.addAttribute("userAvailability", user.getAvailability());
                
                // Convert userAvailability Set<Timeslot> to Set<String> for template usage
                Set<String> userAvailabilityNames = user.getAvailability() == null ? 
                    new HashSet<>() : 
                    user.getAvailability().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
                model.addAttribute("userAvailabilityNames", userAvailabilityNames);
                
                model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
                model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));
                return "request_form";
            }

            Set<Timeslot> timeslotSet = new HashSet<>(timeslots);

            // Create the request
            Request request = requestService.createRequest(user, type, subject, timeslotSet);

            redirectAttributes.addFlashAttribute("message", 
                "Request created successfully! Your " + type.getDisplayName().toLowerCase() + 
                " request for " + subject.getDisplayName() + " has been submitted.");

            return "redirect:/dashboard";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
            model.addAttribute("userAvailability", user.getAvailability());
            
            // Convert userAvailability Set<Timeslot> to Set<String> for template usage
            Set<String> userAvailabilityNames = user.getAvailability() == null ? 
                new HashSet<>() : 
                user.getAvailability().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            model.addAttribute("userAvailabilityNames", userAvailabilityNames);
            
            model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
            model.addAttribute("requestTypes", Arrays.asList(RequestType.values()));
            return "request_form";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating request: " + e.getMessage());
            model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
            model.addAttribute("userAvailability", user.getAvailability());
            
            // Convert userAvailability Set<Timeslot> to Set<String> for template usage
            Set<String> userAvailabilityNames = user.getAvailability() == null ? 
                new HashSet<>() : 
                user.getAvailability().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            model.addAttribute("userAvailabilityNames", userAvailabilityNames);
            
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

    /**
     * Groups user's subjects by category for request creation
     */
    private Map<String, List<Subject>> getGroupedUserSubjects(User user) {
        List<Subject> userSubjects = new ArrayList<>(user.getSubjects());
        return groupSubjectsByCategory(userSubjects);
    }
    
    /**
     * Groups subjects according to requirements:
     * Languages: English, German, French
     * STEM: Mathematics, Physics, Biology, Chemistry  
     * Social Sciences: Economics, Politics, Business
     */
    private Map<String, List<Subject>> getGroupedSubjects() {
        List<Subject> allSubjects = subjectService.getAllSubjects();
        return groupSubjectsByCategory(allSubjects);
    }
    
    /**
     * Helper method to group subjects by category
     */
    private Map<String, List<Subject>> groupSubjectsByCategory(List<Subject> subjects) {
        Map<String, List<Subject>> groups = new HashMap<>();
        
        groups.put("Languages", subjects.stream()
            .filter(s -> s.getDisplayName().equals("English") || 
                        s.getDisplayName().equals("German") || 
                        s.getDisplayName().equals("French"))
            .collect(Collectors.toList()));
            
        groups.put("STEM", subjects.stream()
            .filter(s -> s.getDisplayName().startsWith("Mathematics") || 
                        s.getDisplayName().equals("Physics") || 
                        s.getDisplayName().equals("Biology") || 
                        s.getDisplayName().equals("Chemistry"))
            .collect(Collectors.toList()));
            
        groups.put("Social Sciences", subjects.stream()
            .filter(s -> s.getDisplayName().equals("Economics") || 
                        s.getDisplayName().equals("Politics") || 
                        s.getDisplayName().equals("Business") ||
                        s.getDisplayName().equals("Business Management"))
            .collect(Collectors.toList()));
            
        return groups;
    }
}