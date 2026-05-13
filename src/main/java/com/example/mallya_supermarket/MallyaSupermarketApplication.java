package com.example.mallya_supermarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MallyaSupermarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallyaSupermarketApplication.class, args);
	}

}
