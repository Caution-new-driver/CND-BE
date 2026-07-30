package com.nextrun.cndbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Application {ac

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
