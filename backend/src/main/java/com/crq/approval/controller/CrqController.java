package com.crq.approval.controller;

import com.crq.approval.dto.CrqDto;
import com.crq.approval.dto.DashboardDto;
import com.crq.approval.dto.ProcessingLogDto;
import com.crq.approval.model.ProcessingLog;
import com.crq.approval.service.CrqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/crq")
public class CrqController {

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
     * Ad-hoc trigger: processes CRQs updated after 5:30 PM today.
     * Body: { "triggeredBy": "username" }
     */
    @PostMapping("/adhoc")
    public ResponseEntity<ProcessingLogDto> triggerAdhoc(
            @RequestBody(required = false) Map<String, String> body) {
        String triggeredBy = body != null ? body.getOrDefault("triggeredBy", "UI_USER") : "UI_USER";
        log.info("Ad-hoc CRQ run triggered by: {}", triggeredBy);
        ProcessingLog result = crqService.runAdhocJob(triggeredBy);
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
