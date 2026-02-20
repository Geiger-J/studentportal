package com.example.studentportal.config;

import com.example.studentportal.util.Timeslots;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global model attributes added to every controller's model.
 * Provides label maps for String-based enums and catalog data.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Map<String, String> TYPE_LABELS;
    private static final Map<String, String> STATUS_LABELS;
    private static final Map<String, String> ROLE_LABELS;
    private static final Map<String, String> EXAM_BOARD_LABELS;
    private static final List<String> ALL_STATUSES;

    static {
        TYPE_LABELS = new LinkedHashMap<>();
        TYPE_LABELS.put("TUTOR", "Offering Tutoring");
        TYPE_LABELS.put("TUTEE", "Seeking Tutoring");

        STATUS_LABELS = new LinkedHashMap<>();
        STATUS_LABELS.put("PENDING", "Pending");
        STATUS_LABELS.put("MATCHED", "Matched");
        STATUS_LABELS.put("NOT_MATCHED", "Not Matched");
        STATUS_LABELS.put("DONE", "Done");
        STATUS_LABELS.put("CANCELLED", "Cancelled");

        ROLE_LABELS = new LinkedHashMap<>();
        ROLE_LABELS.put("STUDENT", "Student");
        ROLE_LABELS.put("ADMIN", "Administrator");

        EXAM_BOARD_LABELS = new LinkedHashMap<>();
        EXAM_BOARD_LABELS.put("GCSE", "GCSE");
        EXAM_BOARD_LABELS.put("A_LEVELS", "A Levels");
        EXAM_BOARD_LABELS.put("IB", "International Baccalaureate");
        EXAM_BOARD_LABELS.put("NONE", "None");

        ALL_STATUSES = List.of("PENDING", "MATCHED", "NOT_MATCHED", "DONE", "CANCELLED");
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("typeLabels", TYPE_LABELS);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("roleLabels", ROLE_LABELS);
        model.addAttribute("examBoardLabels", EXAM_BOARD_LABELS);
        model.addAttribute("timeslotLabels", new LinkedHashMap<>(Timeslots.LABELS));
        model.addAttribute("allStatuses", ALL_STATUSES);
    }
}
