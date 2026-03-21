package com.example.studentportal.config;

import com.example.studentportal.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Configuration: post-login redirect based on user role and profile state
//
// Responsibilities:
// - redirect admins to admin dashboard
// - redirect students to dashboard or profile depending on completeness
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal) {
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = (CustomUserDetailsService.CustomUserPrincipal) principal;

            if ("ADMIN".equals(userPrincipal.getUser().getRole())) {
                response.sendRedirect("/admin/dashboard");
            } else if (userPrincipal.getUser().getProfileComplete()) {
                // complete profile -> go straight to dashboard
                response.sendRedirect("/dashboard");
            } else {
                // incomplete profile -> force setup
                response.sendRedirect("/profile");
            }
        } else {
            // unexpected principal type -> fall back to dashboard
            response.sendRedirect("/dashboard");
        }
    }
}