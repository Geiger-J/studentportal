package com.example.studentportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Configuration – entry point for the Student Portal Spring Boot application
 *
 * Responsibilities:
 * - Bootstrap Spring context via @SpringBootApplication
 * - Enable scheduled task support for background jobs
 */
@SpringBootApplication
@EnableScheduling // enables @Scheduled tasks (e.g., status updater)
public class StudentportalApplication {

	public static void main(String[] args) {

		SpringApplication.run(StudentportalApplication.class, args);
	}

}
