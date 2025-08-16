package com.example.studentportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for public pages (landing and about).
 * Handles navigation to non-authenticated pages.
 */
@Controller
public class HomeController {

    /**
     * Landing page - publicly accessible.
     * Shows welcome message and navigation to login/register.
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * About/Contact page - publicly accessible.
     * Combined page showing information about the portal and contact details.
     */
    @GetMapping("/about")
    public String about() {
        return "about";
    }
}