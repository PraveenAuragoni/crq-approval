package com.crq.approval.controller;

import com.crq.approval.dto.CrqDto;
import com.crq.approval.dto.DashboardDto;
import com.crq.approval.dto.ProcessingLogDto;
import com.crq.approval.model.ProcessingLog;
import com.crq.approval.service.CrqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/crq")
public class CrqController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CrqService crqService;

    public CrqController(CrqService crqService) {
        this.crqService = crqService;
    }

    /** Dashboard summary: counts, last runs, recent CRQs and logs */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> getDashboard() {
        return ResponseEntity.ok(crqService.getDashboard());
    }

    /** All CRQs (paginated in future iterations) */
    @GetMapping("/list")
    public ResponseEntity<List<CrqDto>> getAllCrqs() {
        return ResponseEntity.ok(crqService.getAllCrqs());
    }

    /** All processing logs */
    @GetMapping("/logs")
    public ResponseEntity<List<ProcessingLogDto>> getLogs() {
        return ResponseEntity.ok(crqService.getAllLogs());
    }

    /**
     * Ad-hoc trigger: processes CRQs whose lastUpdated falls within [fromDateTime, toDateTime].
     * Body: { "triggeredBy": "username", "fromDateTime": "2024-05-10T17:30:00", "toDateTime": "2024-05-10T23:59:59" }
     * Both fromDateTime and toDateTime are required.
     */
    @PostMapping("/adhoc")
    public ResponseEntity<Object> triggerAdhoc(
            @RequestBody(required = false) Map<String, String> body) {
        String triggeredBy = body != null ? body.getOrDefault("triggeredBy", "UI_USER") : "UI_USER";

        LocalDateTime from;
        LocalDateTime to;
        try {
            String fromRaw = body != null ? body.get("fromDateTime") : null;
            String toRaw   = body != null ? body.get("toDateTime")   : null;
            if (fromRaw == null || toRaw == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "fromDateTime and toDateTime are required."));
            }
            from = LocalDateTime.parse(fromRaw, DT_FMT);
            to   = LocalDateTime.parse(toRaw,   DT_FMT);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid date format. Use ISO format: yyyy-MM-dd'T'HH:mm:ss"));
        }

        if (!from.isBefore(to)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "fromDateTime must be before toDateTime."));
        }

        log.info("Ad-hoc CRQ run triggered by: {} range: {} -> {}", triggeredBy, from, to);
        ProcessingLog result = crqService.runAdhocJob(triggeredBy, from, to);
        return ResponseEntity.ok(ProcessingLogDto.from(result));
    }

    /**
     * Manual scheduled run (for testing / forced re-run).
     * Only use this in non-prod environments or with admin access.
     */
    @PostMapping("/run-now")
    public ResponseEntity<ProcessingLogDto> runNow() {
        log.info("Manual full CRQ run triggered from UI");
        ProcessingLog result = crqService.runScheduledJob();
        return ResponseEntity.ok(ProcessingLogDto.from(result));
    }
}
