package com.example.studentportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Configuration: Spring Boot application entry point
//
// Responsibilities:
// - bootstrap the application context
// - enable the scheduled task executor
@SpringBootApplication
@EnableScheduling
public class StudentportalApplication {

	public static void main(String[] args) {

		SpringApplication.run(StudentportalApplication.class, args);
	}

}
