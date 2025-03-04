package br.com.pulseapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulseapiApplication.class, args);
	}

}
