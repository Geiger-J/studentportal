package com.example.studentportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration – entry point for the Student Portal Spring Boot application
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Bootstrap Spring context via @SpringBootApplication</li>
 *   <li>Enable scheduled task support for background jobs</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling // enables @Scheduled tasks (e.g., status updater)
public class StudentportalApplication {

	public static void main(String[] args) {

		SpringApplication.run(StudentportalApplication.class, args);
	}

}
