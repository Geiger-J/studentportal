package com.example.studentportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration – BCrypt password encoder bean
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>provides PasswordEncoder bean for the security context</li>
 *   <li>uses BCrypt [adaptive, slow-hash algorithm designed for passwords]</li>
 * </ul>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}