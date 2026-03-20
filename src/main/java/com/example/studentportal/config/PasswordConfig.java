package com.example.studentportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * Configuration – BCrypt password encoder bean
 *
 * Responsibilities:
 * - provides PasswordEncoder bean for the security context
 * - uses BCrypt [adaptive, slow-hash algorithm designed for passwords]
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}