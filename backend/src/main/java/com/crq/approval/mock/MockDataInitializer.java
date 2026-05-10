package com.crq.approval.mock;

import com.crq.approval.service.CrqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("mock")
public class MockDataInitializer implements ApplicationRunner {

    private final CrqService crqService;

    public MockDataInitializer(CrqService crqService) {
        this.crqService = crqService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("====================================================");
        log.info("  MOCK MODE: seeding demo data on startup...");
        log.info("====================================================");

        // Simulate the daily 5:30 PM scheduled run
        crqService.runScheduledJob();
        log.info("[MOCK] Scheduled job data seeded.");

        // Simulate one previous ad-hoc run using a range covering post-17:30 today
        LocalDateTime from = LocalDate.now().atTime(17, 30);
        LocalDateTime to = LocalDate.now().atTime(23, 59);
        crqService.runAdhocJob("demo-user", from, to);
        log.info("[MOCK] Ad-hoc job data seeded.");

        log.info("====================================================");
        log.info("  MOCK data ready! Open http://localhost:8080");
        log.info("====================================================");
    }
}
