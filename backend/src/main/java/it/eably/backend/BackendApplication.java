package it.eably.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point.
 * {@code @EnableScheduling} activates the payment-timeout scheduler
 * ({@code PaymentTimeoutScheduler}) which periodically cancels unpaid bookings.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}