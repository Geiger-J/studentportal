package com.example.studentportal.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.SubjectService;
import com.example.studentportal.service.UserService;
import com.example.studentportal.util.Timeslots;

/**
 * Controller for user profile management. Handles profile completion and updates.
 */
@Controller
public class ProfileController {

    private final UserService userService;
    private final SubjectService subjectService;

    @Autowired
    public ProfileController(UserService userService, SubjectService subjectService) {
        this.userService = userService;
        this.subjectService = subjectService;
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getUser();

        if (user.getSubjects() == null) {
            user.setSubjects(new HashSet<>());
        }
        if (user.getAvailability() == null) {
            user.setAvailability(new HashSet<>());
        }

        model.addAttribute("user", user);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectGroups", getGroupedSubjects());
        model.addAttribute("availabilityNames", user.getAvailability());

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            @RequestParam Integer yearGroup,
            @RequestParam(required = false) String examBoard,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = false) List<String> timeslots,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = principal.getUser();

        try {
            if (yearGroup == null || yearGroup < 9 || yearGroup > 13) {
                populateFormModel(model, user, "Year group must be between 9 and 13");
                return "profile";
            }

            user.setYearGroup(yearGroup);

            if (yearGroup >= 12 && yearGroup <= 13) {
                if (examBoard != null && ("A_LEVELS".equals(examBoard) || "IB".equals(examBoard))) {
                    user.setExamBoard(examBoard);
                } else {
                    populateFormModel(model, user, "Please select an exam board (A Levels or IB) for years 12-13");
                    return "profile";
                }
            }

            Set<Subject> selectedSubjects = new HashSet<>();
            if (subjectIds != null && !subjectIds.isEmpty()) {
                for (Long subjectId : subjectIds) {
                    subjectService.findById(subjectId).ifPresent(selectedSubjects::add);
                }
            }
            user.setSubjects(selectedSubjects);

            Set<String> selectedTimeslots = new HashSet<>();
            if (timeslots != null) {
                // Validate slot codes
                for (String slot : timeslots) {
                    if (Timeslots.ALL_CODES_SET.contains(slot)) {
                        selectedTimeslots.add(slot);
                    }
                }
            }
            user.setAvailability(selectedTimeslots);

            userService.updateProfile(user);

            if (user.isProfileComplete()) {
                redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
                return "redirect:/dashboard";
            } else {
                populateFormModel(model, user,
                        "Profile incomplete: Please select at least one subject and one availability slot");
                return "profile";
            }

        } catch (Exception e) {
            populateFormModel(model, user, "Error updating profile: " + e.getMessage());
            return "profile";
        }
    }

    @PostMapping("/profile/delete-account")
    public String deleteAccount(
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
            return "redirect:/profile";
        }
        return "redirect:/";
    }

    private void populateFormModel(Model model, User user, String errorMessage) {
        model.addAttribute("error", errorMessage);
        model.addAttribute("user", user);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("subjectGroups", getGroupedSubjects());
        model.addAttribute("availabilityNames", user.getAvailability() != null ? user.getAvailability() : new HashSet<>());
    }

    private Map<String, List<Subject>> getGroupedSubjects() {
        return groupSubjectsByCategory(subjectService.getAllSubjects());
    }

    private Map<String, List<Subject>> groupSubjectsByCategory(List<Subject> subjects) {
        Map<String, List<Subject>> groups = new HashMap<>();

        List<Subject> languages = subjects.stream()
            .filter(s -> s.getDisplayName().equals("English") ||
                        s.getDisplayName().equals("German") ||
                        s.getDisplayName().equals("French"))
            .collect(Collectors.toList());
        if (!languages.isEmpty()) groups.put("Languages", languages);

        List<Subject> stem = subjects.stream()
            .filter(s -> s.getDisplayName().equals("Mathematics") ||
                        s.getDisplayName().equals("Physics") ||
                        s.getDisplayName().equals("Biology") ||
                        s.getDisplayName().equals("Chemistry"))
            .collect(Collectors.toList());
        if (!stem.isEmpty()) groups.put("STEM", stem);

        List<Subject> social = subjects.stream()
            .filter(s -> s.getDisplayName().equals("Economics") ||
                        s.getDisplayName().equals("Politics") ||
                        s.getDisplayName().equals("Business"))
            .collect(Collectors.toList());
        if (!social.isEmpty()) groups.put("Social Sciences", social);

        return groups;
    }
}
