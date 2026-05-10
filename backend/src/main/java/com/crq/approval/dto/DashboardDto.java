package com.crq.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private int totalToday;
    private int approvedToday;
    private int emailsSentToday;
    private int pendingToday;
    private LocalDateTime lastScheduledRun;
    private LocalDateTime lastAdhocRun;
    private List<CrqDto> recentCrqs;
    private List<ProcessingLogDto> recentLogs;
}
