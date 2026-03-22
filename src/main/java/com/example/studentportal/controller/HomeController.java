package com.example.studentportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Controller: public landing page
//
// - serve the index page without authentication
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() { return "index"; }
}
