package com.example.studentportal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Spring Security configuration for the Student Portal.
 * Configures authentication, authorization, and custom login handling.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    public SecurityConfig(PasswordEncoder passwordEncoder, 
                         AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    /**
     * Configures the security filter chain with custom authentication rules.
     * 
     * Key features:
     * - Permits public access to landing, login, register, about pages and static resources
     * - Requires authentication for all other routes
     * - Custom login page with redirect logic based on profile completeness
     * - Logout redirects to landing page
     * 
     * @param http the HttpSecurity configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Permit public access to these paths
                .requestMatchers("/", "/login", "/register", "/about", 
                                "/css/**", "/images/**", "/js/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                // Use custom success handler for profile-based redirection
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Disable CSRF for simplicity in Phase 1 (should be enabled in production)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Authentication provider using our custom UserDetailsService and password encoder.
     * 
     * @param userDetailsService the custom user details service
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}