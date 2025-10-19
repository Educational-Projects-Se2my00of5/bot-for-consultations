package com.example.botforconsultations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BotForConsultationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotForConsultationsApplication.class, args);
    }

}
