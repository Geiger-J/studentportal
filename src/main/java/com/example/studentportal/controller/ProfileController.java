package com.example.studentportal.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.studentportal.model.ExamBoard;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.Timeslot;
import com.example.studentportal.model.User;
import com.example.studentportal.service.CustomUserDetailsService;
import com.example.studentportal.service.SubjectService;
import com.example.studentportal.service.UserService;

/**
 * Controller for user profile management. Handles profile completion and
 * updates.
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
            // Fallback (should not normally happen if security is configured correctly)
            return "redirect:/login";
        }

        User user = principal.getUser();

        // Ensure defensive non-null collections (in case of legacy data)
        if (user.getSubjects() == null) {
            user.setSubjects(new HashSet<>());
        }
        if (user.getAvailability() == null) {
            user.setAvailability(new HashSet<>());
        }

        model.addAttribute("user", user);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
        model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserPrincipal principal,
            @RequestParam Integer yearGroup,
            @RequestParam(required = false) ExamBoard examBoard,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = false) List<Timeslot> timeslots,
            @RequestParam(defaultValue = "0") Integer maxTutoringPerWeek,
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
                if (examBoard != null && (examBoard == ExamBoard.A_LEVELS || examBoard == ExamBoard.IB)) {
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

            Set<Timeslot> selectedTimeslots = new HashSet<>();
            if (timeslots != null) {
                selectedTimeslots.addAll(timeslots);
            }
            user.setAvailability(selectedTimeslots);

            user.setMaxTutoringPerWeek(maxTutoringPerWeek);

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

    private void populateFormModel(Model model, User user, String errorMessage) {
        model.addAttribute("error", errorMessage);
        model.addAttribute("user", user);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("timeslots", Arrays.asList(Timeslot.values()));
        model.addAttribute("examBoards", Arrays.asList(ExamBoard.values()));
    }
}
