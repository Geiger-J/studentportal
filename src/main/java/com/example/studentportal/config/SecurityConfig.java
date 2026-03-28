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

// Configuration: Spring Security setup for the application
//
// - define URL authorization rules (public, admin, student)
// - configure form login, logout, and access denied handling
// - register DAO authentication provider with BCrypt
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AccessDeniedHandler roleRedirectAccessDeniedHandler;

    @Autowired
    public SecurityConfig(PasswordEncoder passwordEncoder, AuthenticationSuccessHandler authenticationSuccessHandler,
            AccessDeniedHandler roleRedirectAccessDeniedHandler) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.roleRedirectAccessDeniedHandler = roleRedirectAccessDeniedHandler;
    }

    // define URL access rules, form login/logout, and exception handling
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
                // public: no auth required
                .requestMatchers("/", "/login", "/register", "/css/**", "/images/**", "/js/**").permitAll()
                // admin-only area
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // student-only functional pages
                .requestMatchers("/dashboard", "/profile/**", "/requests/**").hasRole("STUDENT")
                // any other authenticated route
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll().successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true"))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
                .exceptionHandling(ex -> ex.accessDeniedHandler(roleRedirectAccessDeniedHandler))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
