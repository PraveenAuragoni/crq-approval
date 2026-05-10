package com.crq.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrqApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrqApprovalApplication.class, args);
    }
}
