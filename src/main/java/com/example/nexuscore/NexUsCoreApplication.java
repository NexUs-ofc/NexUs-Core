package com.example.nexuscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexUsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexUsCoreApplication.class, args);
    }

}
