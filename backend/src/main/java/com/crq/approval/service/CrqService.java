package com.crq.approval.service;

import com.crq.approval.dto.CrqDto;
import com.crq.approval.dto.DashboardDto;
import com.crq.approval.dto.ProcessingLogDto;
import com.crq.approval.model.Crq;
import com.crq.approval.model.ProcessingLog;
import com.crq.approval.repository.CrqRepository;
import com.crq.approval.repository.ProcessingLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CrqService {

    @Value("${crq.adhoc.threshold-time:17:30}")
    private String adhocThresholdTime;

    private final OneDrivePort oneDriveService;
    private final ExcelService excelService;
    private final RemedyPort remedyService;
    private final EmailPort emailService;
    private final CrqRepository crqRepository;
    private final ProcessingLogRepository processingLogRepository;

    public CrqService(OneDrivePort oneDriveService,
                      ExcelService excelService,
                      RemedyPort remedyService,
                      EmailPort emailService,
                      CrqRepository crqRepository,
                      ProcessingLogRepository processingLogRepository) {
        this.oneDriveService = oneDriveService;
        this.excelService = excelService;
        this.remedyService = remedyService;
        this.emailService = emailService;
        this.crqRepository = crqRepository;
        this.processingLogRepository = processingLogRepository;
    }

    /**
     * Full scheduled processing: read all CRQs from OneDrive Excel,
     * check Remedy status, send email for approved ones.
     */
    public ProcessingLog runScheduledJob() {
        return runJob(Crq.BatchType.SCHEDULED, "SCHEDULER", null);
    }

    /**
     * Ad-hoc processing: only processes CRQs updated in Excel AFTER 5:30 PM today.
     * Used for manual triggers from the UI after the scheduled run.
     */
    public ProcessingLog runAdhocJob(String triggeredBy) {
        LocalTime threshold = LocalTime.parse(adhocThresholdTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalDateTime cutoff = LocalDate.now().atTime(threshold);
        log.info("Ad-hoc run: processing CRQs updated after {}", cutoff);
        return runJob(Crq.BatchType.ADHOC, triggeredBy, cutoff);
    }

    private ProcessingLog runJob(Crq.BatchType batchType, String triggeredBy, LocalDateTime updatedAfter) {
        LocalDateTime runAt = LocalDateTime.now();
        int totalRead = 0, approvedCount = 0, emailsSent = 0;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            // 1. Download Excel from OneDrive
            InputStream stream = oneDriveService.downloadExcelFile();

            // 2. Parse Excel rows
            List<ExcelService.ExcelCrqRow> rows = excelService.parseExcel(stream);
            totalRead = rows.size();

            // For ad-hoc: filter to only rows updated after cutoff
            if (updatedAfter != null) {
                rows = rows.stream()
                        .filter(r -> r.getLastUpdated() != null && r.getLastUpdated().isAfter(updatedAfter))
                        .collect(Collectors.toList());
                log.info("Ad-hoc: {} CRQs pass the {} cutoff filter", rows.size(), updatedAfter);
            }

            List<Crq> approvedCrqs = new ArrayList<>();

            // 3. Check each CRQ in Remedy
            for (ExcelService.ExcelCrqRow row : rows) {
                String remedyStatus = remedyService.getCrqStatus(row.getCrqNumber());
                boolean approved = remedyService.isApproved(remedyStatus);

                Crq crq = Crq.builder()
                        .crqNumber(row.getCrqNumber())
                        .title(row.getTitle())
                        .assignee(row.getAssignee())
                        .description(row.getDescription())
                        .remedyStatus(remedyStatus)
                        .approved(approved)
                        .emailSent(false)
                        .processedAt(runAt)
                        .lastUpdatedInExcel(row.getLastUpdated())
                        .batchType(batchType)
                        .batchRunAt(runAt)
                        .build();

                crqRepository.save(crq);

                if (approved) {
                    approvedCrqs.add(crq);
                    approvedCount++;
                }
            }

            // 4. Send one consolidated email for all approved CRQs
            if (!approvedCrqs.isEmpty()) {
                try {
                    emailService.sendApprovalEmail(approvedCrqs);
                    emailsSent = approvedCrqs.size();
                    LocalDateTime now = LocalDateTime.now();
                    for (Crq crq : approvedCrqs) {
                        crq.setEmailSent(true);
                        crq.setEmailSentAt(now);
                        crqRepository.save(crq);
                    }
                } catch (Exception e) {
                    log.error("Email sending failed: {}", e.getMessage());
                    status = "PARTIAL";
                    errorMsg = "Email failed: " + e.getMessage();
                }
            }

        } catch (Exception e) {
            log.error("CRQ job failed: {}", e.getMessage(), e);
            status = "FAILED";
            errorMsg = e.getMessage();
        }

        ProcessingLog logEntry = ProcessingLog.builder()
                .runAt(runAt)
                .batchType(batchType)
                .totalCrqsRead(totalRead)
                .approvedCount(approvedCount)
                .emailsSent(emailsSent)
                .status(status)
                .errorMessage(errorMsg)
                .triggeredBy(triggeredBy)
                .build();

        return processingLogRepository.save(logEntry);
    }

    public DashboardDto getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        List<Crq> todayCrqs = crqRepository.findByProcessedAtBetweenOrderByProcessedAtDesc(startOfDay, endOfDay);
        List<ProcessingLog> recentLogs = processingLogRepository.findTop10ByOrderByRunAtDesc();

        long approvedToday = todayCrqs.stream().filter(Crq::isApproved).count();
        long emailsSentToday = todayCrqs.stream().filter(Crq::isEmailSent).count();
        long pendingToday = todayCrqs.stream().filter(c -> !c.isApproved()).count();

        LocalDateTime lastScheduled = recentLogs.stream()
                .filter(l -> l.getBatchType() == Crq.BatchType.SCHEDULED)
                .map(ProcessingLog::getRunAt)
                .findFirst().orElse(null);

        LocalDateTime lastAdhoc = recentLogs.stream()
                .filter(l -> l.getBatchType() == Crq.BatchType.ADHOC)
                .map(ProcessingLog::getRunAt)
                .findFirst().orElse(null);

        return DashboardDto.builder()
                .totalToday(todayCrqs.size())
                .approvedToday((int) approvedToday)
                .emailsSentToday((int) emailsSentToday)
                .pendingToday((int) pendingToday)
                .lastScheduledRun(lastScheduled)
                .lastAdhocRun(lastAdhoc)
                .recentCrqs(todayCrqs.stream().map(CrqDto::from).collect(Collectors.toList()))
                .recentLogs(recentLogs.stream().map(ProcessingLogDto::from).collect(Collectors.toList()))
                .build();
    }

    public List<CrqDto> getAllCrqs() {
        return crqRepository.findAllByOrderByProcessedAtDesc()
                .stream().map(CrqDto::from).collect(Collectors.toList());
    }

    public List<ProcessingLogDto> getAllLogs() {
        return processingLogRepository.findAllByOrderByRunAtDesc()
                .stream().map(ProcessingLogDto::from).collect(Collectors.toList());
    }
}
