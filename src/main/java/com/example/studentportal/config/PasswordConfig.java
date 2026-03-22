package com.example.studentportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configuration: password encoding bean
//
// - expose BCryptPasswordEncoder as a Spring bean
@Configuration
public class PasswordConfig {

    // BCrypt: adaptive hashing designed for passwords [safe against brute-force]
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}