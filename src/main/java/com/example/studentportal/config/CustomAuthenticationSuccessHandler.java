package com.example.studentportal.config;

import com.example.studentportal.model.Role;
import com.example.studentportal.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication success handler that redirects users based on profile completeness.
 * Implements the business rule: if profile incomplete -> /profile, else -> /dashboard
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        // Get the authenticated user
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetailsService.CustomUserPrincipal) {
            CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (CustomUserDetailsService.CustomUserPrincipal) principal;
            
            // ADMIN users go to admin dashboard
            if (userPrincipal.getUser().getRole() == Role.ADMIN) {
                response.sendRedirect("/admin/dashboard");
            }
            // STUDENT users: check if profile is complete
            else if (userPrincipal.getUser().getProfileComplete()) {
                response.sendRedirect("/dashboard");
            } else {
                response.sendRedirect("/profile");
            }
        } else {
            // Fallback to dashboard if principal type is unexpected
            response.sendRedirect("/dashboard");
        }
    }
}