package com.example.clubmanagementbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClubManagementBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClubManagementBackendApplication.class, args);
    }

}
