package com.example.studentportal.config;

import com.example.studentportal.util.Timeslots;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

/**
 * Global model attributes added to every controller's model.
 * Provides label maps for String-based enums and catalog data.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Map<String, String> TYPE_LABELS = Map.of(
        "TUTOR", "Offering Tutoring",
        "TUTEE", "Seeking Tutoring"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "PENDING", "Pending",
        "MATCHED", "Matched",
        "NOT_MATCHED", "Not Matched",
        "DONE", "Done",
        "CANCELLED", "Cancelled"
    );

    private static final Map<String, String> ROLE_LABELS = Map.of(
        "STUDENT", "Student",
        "ADMIN", "Administrator"
    );

    private static final Map<String, String> EXAM_BOARD_LABELS = Map.of(
        "GCSE", "GCSE",
        "A_LEVELS", "A Levels",
        "IB", "International Baccalaureate",
        "NONE", "None"
    );

    private static final List<String> ALL_STATUSES = List.of(
        "PENDING", "MATCHED", "NOT_MATCHED", "DONE", "CANCELLED"
    );

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("typeLabels", TYPE_LABELS);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("roleLabels", ROLE_LABELS);
        model.addAttribute("examBoardLabels", EXAM_BOARD_LABELS);
        model.addAttribute("timeslotLabels", Timeslots.LABELS);
        model.addAttribute("allStatuses", ALL_STATUSES);
    }
}
