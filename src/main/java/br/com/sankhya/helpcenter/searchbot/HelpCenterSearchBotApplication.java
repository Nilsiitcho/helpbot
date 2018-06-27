package br.com.sankhya.helpcenter.searchbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class HelpCenterSearchBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpCenterSearchBotApplication.class, args);
	}

}
