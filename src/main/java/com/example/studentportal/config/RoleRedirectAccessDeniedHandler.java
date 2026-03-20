package com.example.studentportal.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Configuration - redirects on access denied based on authenticated role
//
// Responsibilities:
// - sends ADMIN to /admin/dashboard on 403
// - sends STUDENT to /dashboard on 403
// - sends unauthenticated users to /login
@Component
public class RoleRedirectAccessDeniedHandler implements AccessDeniedHandler {

    // redirect to role-appropriate page instead of generic 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            // branch on role
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/dashboard");
            }
        } else {
            response.sendRedirect("/login");
        }
    }
}