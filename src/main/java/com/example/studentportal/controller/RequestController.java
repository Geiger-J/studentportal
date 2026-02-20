package com.example.studentportal.controller;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.RequestService;
import com.example.studentportal.service.SubjectService;
import com.example.studentportal.util.Timeslots;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for tutoring request management.
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

    @GetMapping("/new")
    public String showRequestForm(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                                 Model model) {

        User user = principal.getUser();

        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        Set<String> userAvailabilityNames = user.getAvailability() != null
                ? user.getAvailability() : new HashSet<>();

        model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
        model.addAttribute("userAvailabilityNames", userAvailabilityNames);
        model.addAttribute("requestTypes", List.of("TUTOR", "TUTEE"));

        return "request_form";
    }

    @PostMapping
    public String createRequest(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @RequestParam String type,
                               @RequestParam Long subjectId,
                               @RequestParam(required = false) List<String> timeslots,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        User user = principal.getUser();

        if (!user.getProfileComplete()) {
            return "redirect:/profile";
        }

        try {
            Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject selected"));

            if (timeslots == null || timeslots.isEmpty()) {
                populateRequestFormModel(model, user);
                model.addAttribute("error", "Please select at least one timeslot");
                return "request_form";
            }

            // Validate and filter timeslot codes
            Set<String> timeslotSet = timeslots.stream()
                    .filter(Timeslots.ALL_CODES_SET::contains)
                    .collect(Collectors.toCollection(HashSet::new));

            if (timeslotSet.isEmpty()) {
                populateRequestFormModel(model, user);
                model.addAttribute("error", "Please select at least one valid timeslot");
                return "request_form";
            }

            Request request = requestService.createRequest(user, type, subject, timeslotSet);

            String typeLabel = "TUTOR".equals(type) ? "offering tutoring" : "seeking tutoring";
            redirectAttributes.addFlashAttribute("message",
                "Request created successfully! Your " + typeLabel +
                " request for " + subject.getDisplayName() + " has been submitted.");

            return "redirect:/dashboard";

        } catch (IllegalArgumentException e) {
            populateRequestFormModel(model, user);
            model.addAttribute("error", e.getMessage());
            return "request_form";
        } catch (Exception e) {
            populateRequestFormModel(model, user);
            model.addAttribute("error", "Error creating request: " + e.getMessage());
            return "request_form";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @PathVariable Long id,
                               RedirectAttributes redirectAttributes) {

        User user = principal.getUser();

        try {
            Request cancelledRequest = requestService.cancelRequest(id, user);

            String typeLabel = "TUTOR".equals(cancelledRequest.getType()) ? "offering tutoring" : "seeking tutoring";
            redirectAttributes.addFlashAttribute("message",
                "Request cancelled successfully. Your " + typeLabel +
                " request for " + cancelledRequest.getSubject().getDisplayName() +
                " has been cancelled.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling request: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    private void populateRequestFormModel(Model model, User user) {
        Set<String> userAvailabilityNames = user.getAvailability() != null
                ? user.getAvailability() : new HashSet<>();
        model.addAttribute("subjectGroups", getGroupedUserSubjects(user));
        model.addAttribute("userAvailabilityNames", userAvailabilityNames);
        model.addAttribute("requestTypes", List.of("TUTOR", "TUTEE"));
    }

    private Map<String, List<Subject>> getGroupedUserSubjects(User user) {
        return groupSubjectsByCategory(new ArrayList<>(user.getSubjects()));
    }

    private Map<String, List<Subject>> groupSubjectsByCategory(List<Subject> subjects) {
        Map<String, List<Subject>> groups = new HashMap<>();

        groups.put("Languages", subjects.stream()
            .filter(s -> s.getDisplayName().equals("English") ||
                        s.getDisplayName().equals("German") ||
                        s.getDisplayName().equals("French"))
            .collect(Collectors.toList()));

        groups.put("STEM", subjects.stream()
            .filter(s -> s.getDisplayName().equals("Mathematics") ||
                        s.getDisplayName().equals("Physics") ||
                        s.getDisplayName().equals("Biology") ||
                        s.getDisplayName().equals("Chemistry"))
            .collect(Collectors.toList()));

        groups.put("Social Sciences", subjects.stream()
            .filter(s -> s.getDisplayName().equals("Economics") ||
                        s.getDisplayName().equals("Politics") ||
                        s.getDisplayName().equals("Business"))
            .collect(Collectors.toList()));

        return groups;
    }
}
