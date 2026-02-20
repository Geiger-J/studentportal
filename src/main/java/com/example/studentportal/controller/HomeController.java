package com.example.studentportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for public pages.
 */
@Controller
public class HomeController {

    /**
     * Landing page - publicly accessible.
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
