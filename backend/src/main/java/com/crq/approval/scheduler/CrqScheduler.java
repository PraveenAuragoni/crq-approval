package com.crq.approval.scheduler;

import com.crq.approval.model.ProcessingLog;
import com.crq.approval.service.CrqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CrqScheduler {

    private final CrqService crqService;

    public CrqScheduler(CrqService crqService) {
        this.crqService = crqService;
    }

    /**
     * Runs every day at 5:30 PM (cron configurable via application.properties).
     * Reads Excel from OneDrive, checks Remedy API, sends emails for approved CRQs.
     */
    @Scheduled(cron = "${crq.scheduler.cron}")
    public void scheduledCrqRun() {
        log.info("===== CRQ Scheduled Job Starting =====");
        try {
            ProcessingLog result = crqService.runScheduledJob();
            log.info("===== CRQ Scheduled Job Completed: status={}, total={}, approved={}, emails={}",
                    result.getStatus(), result.getTotalCrqsRead(),
                    result.getApprovedCount(), result.getEmailsSent());
        } catch (Exception e) {
            log.error("===== CRQ Scheduled Job FAILED: {}", e.getMessage(), e);
        }
    }
}
