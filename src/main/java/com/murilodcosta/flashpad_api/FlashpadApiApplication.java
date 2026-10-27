package com.murilodcosta.flashpad_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashpadApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlashpadApiApplication.class, args);
	}

}
