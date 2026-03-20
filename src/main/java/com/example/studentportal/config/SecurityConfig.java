package com.example.studentportal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

// Configuration - Spring Security filter chain and authentication provider configuration
//
// Responsibilities:
// - defines public, student-only, and admin-only URL access rules
// - configures form login with custom success handler
// - registers role-based access-denied handler
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // enables @PreAuthorize on controllers
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AccessDeniedHandler roleRedirectAccessDeniedHandler;

    @Autowired
    public SecurityConfig(PasswordEncoder passwordEncoder,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AccessDeniedHandler roleRedirectAccessDeniedHandler) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.roleRedirectAccessDeniedHandler = roleRedirectAccessDeniedHandler;
    }

    // configure URL access rules, login, logout, and error handling
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
                // Public
                .requestMatchers("/", "/login", "/register", "/css/**", "/images/**", "/js/**")
                .permitAll()
                // Admin-only area
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Student-only functional pages
                .requestMatchers("/dashboard", "/profile/**", "/requests/**").hasRole("STUDENT")
                // Any other authenticated route (tighten if desired)
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll()
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true"))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(roleRedirectAccessDeniedHandler))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // wire DAO auth with our UserDetailsService and BCrypt encoder
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
