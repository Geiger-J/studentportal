package com.example.studentportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller – landing page for the public-facing index
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>serves the public index page</li>
 * </ul>
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() { return "index"; }
}
