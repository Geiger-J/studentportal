package com.example.studentportal.controller;

import com.example.studentportal.model.*;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.SubjectService;
import com.example.studentportal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for user profile management.
 * Handles profile completion and updates.
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

    /**
     * Shows the profile completion/editing page.
     * Redirects to dashboard if profile is already complete.
     */
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                             Model model) {
        
        User user = principal.getUser();
        
        // Add data for form
        model.addAttribute("user", user);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
        model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));

        return "profile";
    }

    /**
     * Processes profile updates.
     * Validates data and saves profile changes.
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
                               @RequestParam Integer yearGroup,
                               @RequestParam(required = false) ExamBoard examBoard,
                               @RequestParam(required = false) List<Long> subjectIds,
                               @RequestParam(required = false) List<Timeslot> timeslots,
                               @RequestParam(defaultValue = "0") Integer maxTutoringPerWeek,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        User user = principal.getUser();

        try {
            // Validate year group
            if (yearGroup == null || yearGroup < 9 || yearGroup > 13) {
                model.addAttribute("error", "Year group must be between 9 and 13");
                model.addAttribute("user", user);
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
                model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));
                return "profile";
            }

            // Update year group (this will auto-set exam board for years 9-11)
            user.setYearGroup(yearGroup);

            // Handle exam board for years 12-13
            if (yearGroup >= 12 && yearGroup <= 13) {
                if (examBoard != null && (examBoard == ExamBoard.A_LEVELS || examBoard == ExamBoard.IB)) {
                    user.setExamBoard(examBoard);
                } else {
                    model.addAttribute("error", "Please select an exam board (A Levels or IB) for years 12-13");
                    model.addAttribute("user", user);
                    model.addAttribute("subjects", subjectService.getAllSubjects());
                    model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
                    model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));
                    return "profile";
                }
            }

            // Update subjects
            Set<Subject> selectedSubjects = new HashSet<>();
            if (subjectIds != null && !subjectIds.isEmpty()) {
                for (Long subjectId : subjectIds) {
                    subjectService.findById(subjectId).ifPresent(selectedSubjects::add);
                }
            }
            user.setSubjects(selectedSubjects);

            // Update availability
            Set<Timeslot> selectedTimeslots = new HashSet<>();
            if (timeslots != null) {
                selectedTimeslots.addAll(timeslots);
            }
            user.setAvailability(selectedTimeslots);

            // Update max tutoring per week
            user.setMaxTutoringPerWeek(maxTutoringPerWeek);

            // Save and update profile completeness
            userService.updateProfile(user);

            if (user.isProfileComplete()) {
                redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Profile incomplete: Please select at least one subject and one availability slot");
                model.addAttribute("user", user);
                model.addAttribute("subjects", subjectService.getAllSubjects());
                model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
                model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));
                return "profile";
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
            model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));
            return "profile";
        }
    }
}