package br.com.pulseapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.pulseapi.service.PlanoService;

@SpringBootApplication
@EnableScheduling
public class PulseapiApplication implements CommandLineRunner{

	@Autowired
    private PlanoService planService;

	public static void main(String[] args) {
		SpringApplication.run(PulseapiApplication.class, args);
	}

	@Override
    public void run(String... args) throws Exception {
        planService.initializePlans();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
