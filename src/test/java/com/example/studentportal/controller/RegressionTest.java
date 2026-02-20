package com.example.studentportal.controller;

import com.example.studentportal.model.Role;
import com.example.studentportal.model.User;
import com.example.studentportal.repository.UserRepository;
import com.example.studentportal.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression tests ensuring core security and page-access behaviour remains intact.
 * Complements SecurityConfigTest with authenticated-user scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User studentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        studentUser = new User("Test Student", "1student@bromsgrove-school.co.uk",
                passwordEncoder.encode("password"), Role.STUDENT);
        studentUser.setProfileComplete(true);
        studentUser = userRepository.save(studentUser);

        adminUser = new User("Admin User", "admin@bromsgrove-school.co.uk",
                passwordEncoder.encode("password"), Role.ADMIN);
        adminUser = userRepository.save(adminUser);
    }

    // ── Unauthenticated redirect tests ────────────────────────────────────────

    @Test
    void unauthenticatedRequestToAdminDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Authenticated access tests ────────────────────────────────────────────

    @Test
    void authenticatedStudentCanAccessDashboard() throws Exception {
        CustomUserDetailsService.CustomUserPrincipal principal =
                new CustomUserDetailsService.CustomUserPrincipal(studentUser);

        mockMvc.perform(get("/dashboard")
                        .with(user(principal)))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedAdminCanAccessAdminDashboard() throws Exception {
        CustomUserDetailsService.CustomUserPrincipal principal =
                new CustomUserDetailsService.CustomUserPrincipal(adminUser);

        mockMvc.perform(get("/admin/dashboard")
                        .with(user(principal)))
                .andExpect(status().isOk());
    }
}
