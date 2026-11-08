package com.castfan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CastFanApplication {

	public static void main(String[] args) {
		SpringApplication.run(CastFanApplication.class, args);
	}

}
