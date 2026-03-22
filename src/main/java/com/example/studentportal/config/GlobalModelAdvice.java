package com.example.studentportal.config;

import com.example.studentportal.util.Timeslots;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Configuration: injects global model attributes into every view
//
// - provide label maps for request type, status, role, and exam board
// - expose timeslot labels and status list to all templates
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Map<String, String> TYPE_LABELS; // TUTOR/TUTEE -> display text
    private static final Map<String, String> STATUS_LABELS; // PENDING/MATCHED/DONE/CANCELLED ->
                                                            // display text
    private static final Map<String, String> ROLE_LABELS; // STUDENT/ADMIN -> display text
    private static final Map<String, String> EXAM_BOARD_LABELS; // GCSE/A_LEVELS/IB/NONE -> display
                                                                // text
    private static final List<String> ALL_STATUSES; // ordered status list for filter dropdowns

    static {
        TYPE_LABELS = new LinkedHashMap<>();
        TYPE_LABELS.put("TUTOR", "Offering Tutoring");
        TYPE_LABELS.put("TUTEE", "Seeking Tutoring");

        STATUS_LABELS = new LinkedHashMap<>();
        STATUS_LABELS.put("PENDING", "Pending");
        STATUS_LABELS.put("MATCHED", "Matched");
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

        ALL_STATUSES = List.of("PENDING", "MATCHED", "DONE", "CANCELLED");
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
