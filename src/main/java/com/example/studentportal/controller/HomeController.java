package com.example.studentportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
 * Controller – landing page for the public-facing index
 *
 * Responsibilities:
 * - serves the public index page
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() { return "index"; }
}
