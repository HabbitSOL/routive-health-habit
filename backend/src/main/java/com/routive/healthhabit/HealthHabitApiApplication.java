package com.routive.healthhabit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthHabitApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthHabitApiApplication.class, args);
	}

}
